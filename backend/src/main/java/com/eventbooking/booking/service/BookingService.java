package com.eventbooking.booking.service;

import com.eventbooking.booking.dto.HoldRequest;
import com.eventbooking.booking.dto.HoldResponse;
import com.eventbooking.booking.entity.Booking;
import com.eventbooking.booking.entity.BookingSeat;
import com.eventbooking.booking.lock.SeatLockService;
import com.eventbooking.booking.repository.BookingRepository;
import com.eventbooking.booking.repository.BookingSeatRepository;
import com.eventbooking.catalog.seat.Seat;
import com.eventbooking.catalog.seat.SeatRepository;
import com.eventbooking.catalog.show.Show;
import com.eventbooking.catalog.show.ShowRepository;
import com.eventbooking.common.exception.ResourceNotFoundException;
import com.eventbooking.common.exception.SeatAlreadyHeldException;
import com.eventbooking.common.exception.UnauthorizedActionException;
import com.eventbooking.user.entity.User;
import com.eventbooking.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing bookings.
 * Implements the seat hold flow with Redis locking and database persistence.
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final long HOLD_DURATION_SECONDS = 300; // 5 minutes

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final UserRepository userRepository;
    private final SeatLockService seatLockService;

    public BookingService(BookingRepository bookingRepository,
                         BookingSeatRepository bookingSeatRepository,
                         SeatRepository seatRepository,
                         ShowRepository showRepository,
                         UserRepository userRepository,
                         SeatLockService seatLockService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.seatLockService = seatLockService;
    }

    /**
     * Hold seats for a user.
     * Flow:
     * 1. Validate show and seats exist and are available
     * 2. Persist a PENDING booking in the database (JPA generates the UUID)
     * 3. Atomically acquire Redis locks using the generated booking ID (all-or-nothing)
     * 4. If lock acquisition fails, throw so the enclosing transaction rolls back the DB insert
     *
     * @param request Hold request with showId and seatIds
     * @param userId  The user making the booking
     * @return HoldResponse with booking ID and expiry
     */
    @Transactional
    public HoldResponse holdSeats(HoldRequest request, Long userId) {
        Long showId = request.getShowId();
        List<Long> seatIds = request.getSeatIds();

        // 1. Validate show exists
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));

        if ("CANCELLED".equals(show.getStatus())) {
            throw new IllegalArgumentException("Cannot book seats for a cancelled show");
        }

        // 2. Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // 3. Fetch and validate seats
        List<Seat> seats = seatRepository.findAllById(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("One or more seats not found");
        }

        // Verify all seats belong to this show and are AVAILABLE
        for (Seat seat : seats) {
            if (!seat.getShow().getId().equals(showId)) {
                throw new IllegalArgumentException("Seat " + seat.getId() + " does not belong to show " + showId);
            }
            if (!"AVAILABLE".equals(seat.getStatus())) {
                throw new IllegalArgumentException("Seat " + seat.getId() + " is not available (status: " + seat.getStatus() + ")");
            }
        }

        // 4. Calculate total amount
        BigDecimal totalAmount = seats.stream()
                .map(Seat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Persist booking first so JPA generates the UUID cleanly
        Instant expiresAt = Instant.now().plusSeconds(HOLD_DURATION_SECONDS);
        Booking booking = new Booking(user, show, totalAmount, expiresAt);
        for (Seat seat : seats) {
            booking.addBookingSeat(new BookingSeat(booking, seat, seat.getPrice()));
        }
        booking = bookingRepository.save(booking);  // use returned instance — merge() returns the managed copy
        bookingRepository.flush();  // force INSERT so the generated UUID is visible

        UUID bookingId = booking.getId();

        // 6. Atomically acquire Redis locks using the generated booking ID
        SeatLockService.LockResult lockResult = seatLockService.acquireLocks(showId, seatIds, bookingId);

        if (!lockResult.isSuccess()) {
            log.warn("Failed to acquire locks for seats {} in show {}: {}", seatIds, showId, lockResult.getFailureReason());
            // Throwing here causes the @Transactional to roll back the DB insert automatically
            throw new SeatAlreadyHeldException("one or more seats");
        }

        log.info("Created PENDING booking {} for user {} with {} seats", bookingId, userId, seatIds.size());

        return new HoldResponse(
                bookingId,
                showId,
                seatIds,
                totalAmount,
                expiresAt,
                "PENDING"
        );
    }

    /**
     * Get booking by ID.
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));
    }

    /**
     * Get all bookings for a user.
     */
    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Cancel a booking.
     * Releases Redis locks if the booking is still PENDING.
     */
    @Transactional
    public void cancelBooking(UUID bookingId, Long userId) {
        Booking booking = getBookingById(bookingId);

        // Verify ownership
        if (!booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only cancel your own bookings");
        }

        // Get seat IDs before cancellation
        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();

        booking.cancel();
        bookingRepository.save(booking);

        // Release Redis locks if they still exist
        if (booking.isPending() || "CANCELLED".equals(booking.getStatus())) {
            int released = seatLockService.releaseLocks(booking.getShow().getId(), seatIds, bookingId);
            log.info("Cancelled booking {} and released {} locks", bookingId, released);
        }
    }
}
