package com.eventbooking.booking.service;

import com.eventbooking.booking.dto.BookingDetailDto;
import com.eventbooking.booking.dto.BookingSummaryDto;
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
import com.eventbooking.common.exception.BookingExpiredException;
import com.eventbooking.common.exception.PaymentFailedException;
import com.eventbooking.common.exception.ResourceNotFoundException;
import com.eventbooking.common.exception.SeatAlreadyHeldException;
import com.eventbooking.common.exception.UnauthorizedActionException;
import com.eventbooking.payment.entity.Payment;
import com.eventbooking.payment.repository.PaymentRepository;
import com.eventbooking.payment.service.PaymentService;
import com.eventbooking.user.entity.User;
import com.eventbooking.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
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
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public BookingService(BookingRepository bookingRepository,
                         BookingSeatRepository bookingSeatRepository,
                         SeatRepository seatRepository,
                         ShowRepository showRepository,
                         UserRepository userRepository,
                         SeatLockService seatLockService,
                         PaymentRepository paymentRepository,
                         PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.userRepository = userRepository;
        this.seatLockService = seatLockService;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
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
     * Get booking details for a booking.
     */
    @Transactional(readOnly = true)
    public BookingDetailDto getBookingDetails(UUID bookingId, Long userId) {
        Booking booking = getBookingById(bookingId);

        // Verify ownership
        if (!booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only view your own bookings");
        }

        String paymentStatus = paymentRepository.findByBookingId(bookingId)
                .map(Payment::getStatus)
                .orElse("NONE");

        List<com.eventbooking.booking.dto.BookingSeatDto> seatDtos = booking.getBookingSeats().stream()
                .map(bs -> new com.eventbooking.booking.dto.BookingSeatDto(
                        bs.getSeat().getId(),
                        bs.getSeat().getSection(),
                        bs.getSeat().getRowLabel(),
                        bs.getSeat().getSeatNumber(),
                        bs.getPriceAtBooking(),
                        bs.getActive()
                ))
                .toList();

        return new BookingDetailDto(
                booking.getId(),
                booking.getShow().getEvent().getTitle(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getCreatedAt(),
                booking.getExpiresAt(),
                paymentStatus,
                seatDtos
        );
    }

    /**
     * Get all bookings for a user.
     */
    @Transactional(readOnly = true)
    public Page<BookingSummaryDto> getBookingsForUser(Long userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable)
                .map(b -> new BookingSummaryDto(
                        b.getId(),
                        b.getShow().getEvent().getTitle(),
                        b.getStatus(),
                        b.getTotalAmount(),
                        b.getCreatedAt()
                ));
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

    /**
     * Confirm a booking after successful payment.
     * Flow: validate → payment → confirm booking → mark seats BOOKED → release Redis locks.
     */
    @Transactional
    public void confirmBooking(UUID bookingId, Long userId) {
        Booking booking = getBookingById(bookingId);

        // Verify ownership
        if (!booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException("You can only confirm your own bookings");
        }

        // Must be PENDING
        if (!booking.isPending()) {
            throw new IllegalStateException("Booking is not in PENDING state: " + booking.getStatus());
        }

        // Must not be expired
        if (booking.isExpired()) {
            throw new BookingExpiredException(bookingId);
        }

        // Process payment
        String idempotencyKey = bookingId.toString();
        Payment payment = new Payment(bookingId, booking.getTotalAmount(), idempotencyKey);
        paymentRepository.save(payment);

        com.eventbooking.payment.dto.PaymentResult result = paymentService.processPayment(bookingId, idempotencyKey);

        if (!result.success()) {
            payment.fail(result.failureReason());
            paymentRepository.save(payment);
            booking.cancel();
            bookingRepository.save(booking);

            // Release Redis locks
            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatLockService.releaseLocks(booking.getShow().getId(), seatIds, bookingId);
            throw new PaymentFailedException(result.failureReason());
        }

        // Payment succeeded — confirm the booking
        payment.succeed();
        paymentRepository.save(payment);

        booking.confirm();
        for (BookingSeat bs : booking.getBookingSeats()) {
            bs.activate();
            bs.getSeat().setStatus("BOOKED");
        }
        bookingRepository.save(booking);

        // Release Redis locks — seats are now permanently booked
        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();
        seatLockService.releaseLocks(booking.getShow().getId(), seatIds, bookingId);
    }

    /**
     * Expire all PENDING bookings that have passed their expiry time.
     * Called by @Scheduled to clean up stale holds.
     */
    @Transactional
    @Scheduled(fixedDelayString = "${app.booking.expiry-sweeper-interval:60000}") // every minute by default
    public void expirePendingBookings() {
        Instant now = Instant.now();
        List<Booking> expired = bookingRepository.findExpiredPendingBookings(now);

        for (Booking booking : expired) {
            try {
                booking.expire(); // sets status to EXPIRED
                bookingRepository.save(booking);

                // Release Redis locks
                List<Long> seatIds = booking.getBookingSeats().stream()
                        .map(bs -> bs.getSeat().getId())
                        .toList();
                int released = seatLockService.releaseLocks(booking.getShow().getId(), seatIds, booking.getId());
                log.info("Expired booking {} and released {} locks", booking.getId(), released);
            } catch (Exception e) {
                log.error("Failed to expire booking {}: {}", booking.getId(), e.getMessage(), e);
            }
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} pending bookings", expired.size());
        }
    }
}
