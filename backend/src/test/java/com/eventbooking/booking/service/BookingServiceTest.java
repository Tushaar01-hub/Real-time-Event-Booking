package com.eventbooking.booking.service;

import com.eventbooking.booking.dto.HoldRequest;
import com.eventbooking.booking.dto.HoldResponse;
import com.eventbooking.booking.entity.Booking;
import com.eventbooking.booking.lock.SeatLockService;
import com.eventbooking.booking.repository.BookingRepository;
import com.eventbooking.catalog.category.Category;
import com.eventbooking.catalog.category.CategoryRepository;
import com.eventbooking.catalog.event.Event;
import com.eventbooking.catalog.event.EventRepository;
import com.eventbooking.catalog.seat.Seat;
import com.eventbooking.catalog.seat.SeatRepository;
import com.eventbooking.catalog.show.Show;
import com.eventbooking.catalog.show.ShowRepository;
import com.eventbooking.common.exception.BookingExpiredException;
import com.eventbooking.common.exception.PaymentFailedException;
import com.eventbooking.common.exception.SeatAlreadyHeldException;
import com.eventbooking.payment.entity.Payment;
import com.eventbooking.payment.repository.PaymentRepository;
import com.eventbooking.payment.service.PaymentService;
import com.eventbooking.support.AbstractIntegrationTest;
import com.eventbooking.user.entity.User;
import com.eventbooking.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.eventbooking.booking.entity.BookingSeat;
import com.eventbooking.payment.repository.PaymentRepository;
import com.eventbooking.payment.service.PaymentService;
/**
 * Integration tests for BookingService holdSeats flow.
 * Tests the full flow: validation → Redis locks → DB persistence → compensation.
 */
@Transactional
class BookingServiceTest extends AbstractIntegrationTest {
//    @Mock
//    private SeatLockService seatLockService;
@Autowired
private PaymentService paymentService;

@Autowired
private PaymentRepository paymentRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ShowRepository showRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Show testShow;
    private Seat seat1;
    private Seat seat2;
    private Seat seat3;

    @BeforeEach
    void setUp() {
        // Clear Redis locks
        clearAllLocks();

        // Create test user using constructor
        testUser = new User("test@example.com", passwordEncoder.encode("password"),
                           "Test User", com.eventbooking.common.security.Role.USER);
        testUser = userRepository.save(testUser);

        // Create category using constructor
        Category category = new Category("Music", "music");
        category = categoryRepository.save(category);

        // Create event - need to check if it has setters
        Event event = eventRepository.save(createEvent(category, "Test Concert", "Test Venue"));

        // Create show - need to check if it has setters
        testShow = showRepository.save(createShow(event, "Main Hall"));

        // Create seats using constructor
        seat1 = new Seat(testShow, "A", 1, "VIP", new BigDecimal("100.00"));
        seat2 = new Seat(testShow, "A", 2, "VIP", new BigDecimal("100.00"));
        seat3 = new Seat(testShow, "A", 3, "VIP", new BigDecimal("100.00"));
        seat1 = seatRepository.save(seat1);
        seat2 = seatRepository.save(seat2);
        seat3 = seatRepository.save(seat3);
    }

    private Event createEvent(Category category, String title, String location) {
        return new Event(
                title,
                "A test event",
                category,
                location,
                null
        );
    }

    private Show createShow(Event event, String venue) {
        Instant startTime = Instant.now().plus(7, ChronoUnit.DAYS);
        Instant endTime = startTime.plus(2, ChronoUnit.HOURS);

        return new Show(
                event,
                startTime,
                endTime,
                venue
        );
    }

    @Test
    void holdSeats_Success_CreatesBookingAndAcquiresLocks() {
        // Given
        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId(), seat2.getId()));

        // When
        HoldResponse response = bookingService.holdSeats(request, testUser.getId());

        // Then
        assertThat(response.getBookingId()).isNotNull();
        assertThat(response.getShowId()).isEqualTo(testShow.getId());
        assertThat(response.getSeatIds()).containsExactlyInAnyOrder(seat1.getId(), seat2.getId());
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getExpiresAt()).isAfter(Instant.now());

        // Verify booking exists in DB
        Booking booking = bookingRepository.findById(response.getBookingId()).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo("PENDING");
        assertThat(booking.getBookingSeats()).hasSize(2);
        assertThat(booking.getBookingSeats()).allMatch(bs -> !bs.getActive()); // Not active until confirmed

        // Verify Redis locks are acquired
        assertThat(seatLockService.isLocked(testShow.getId(), seat1.getId())).isTrue();
        assertThat(seatLockService.isLocked(testShow.getId(), seat2.getId())).isTrue();
        assertThat(seatLockService.getLockOwner(testShow.getId(), seat1.getId())).isEqualTo(response.getBookingId());
    }

    @Test
    void holdSeats_Failure_WhenSeatAlreadyHeld() {
        // Given - First user holds seat1
        HoldRequest firstRequest = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        bookingService.holdSeats(firstRequest, testUser.getId());

        // Create second user
        User secondUser = new User("user2@example.com", passwordEncoder.encode("password"),
                                  "Second User", com.eventbooking.common.security.Role.USER);
        secondUser = userRepository.save(secondUser);

        // When - Second user tries to hold seat1 and seat2
        HoldRequest secondRequest = new HoldRequest(testShow.getId(), List.of(seat1.getId(), seat2.getId()));

        // Then - Should throw SeatAlreadyHeldException
        final Long secondUserId = secondUser.getId();

        assertThatThrownBy(() -> bookingService.holdSeats(secondRequest, secondUserId))
                .isInstanceOf(SeatAlreadyHeldException.class);

        // Verify seat2 was NOT locked (all-or-nothing)
        assertThat(seatLockService.isLocked(testShow.getId(), seat2.getId())).isFalse();
    }

    @Test
    void holdSeats_Failure_WhenSeatNotAvailable() {
        // Given - Mark seat1 as BLOCKED
        seat1.setStatus("BLOCKED");
        seatRepository.save(seat1);

        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));

        // When/Then
        assertThatThrownBy(() -> bookingService.holdSeats(request, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");

        // Verify no locks were acquired
        assertThat(seatLockService.isLocked(testShow.getId(), seat1.getId())).isFalse();
    }

    @Test
    void holdSeats_Failure_WhenShowCancelled() {
        // Given
        testShow.setStatus("CANCELLED");
        showRepository.save(testShow);

        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));

        // When/Then
        assertThatThrownBy(() -> bookingService.holdSeats(request, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled show");
    }

    @Test
    void holdSeats_Failure_WhenSeatBelongsToDifferentShow() {
        // Given - Create a different show
        Show anotherShow = createShow(testShow.getEvent(), "Another Hall");
        anotherShow = showRepository.save(anotherShow);

        // When - Try to book seat1 (which belongs to testShow) for anotherShow
        HoldRequest request = new HoldRequest(anotherShow.getId(), List.of(seat1.getId()));

        // Then
        assertThatThrownBy(() -> bookingService.holdSeats(request, testUser.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to show");
    }

    @Test
    void cancelBooking_Success_ReleasesLocksAndMarksCancelled() {
        // Given - Create a booking
        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        HoldResponse holdResponse = bookingService.holdSeats(request, testUser.getId());

        // When
        bookingService.cancelBooking(holdResponse.getBookingId(), testUser.getId());

        // Then
        Booking booking = bookingRepository.findById(holdResponse.getBookingId()).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo("CANCELLED");

        // Verify locks are released
        assertThat(seatLockService.isLocked(testShow.getId(), seat1.getId())).isFalse();
    }

    @Test
    void cancelBooking_Failure_WhenNotOwner() {
        // Given - User1 creates a booking
        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        HoldResponse holdResponse = bookingService.holdSeats(request, testUser.getId());

        // Create second user
        User anotherUser = new User("another@example.com", passwordEncoder.encode("password"),
                                   "Another User", com.eventbooking.common.security.Role.USER);
        anotherUser = userRepository.save(anotherUser);

        // When/Then - User2 tries to cancel User1's booking
        final Long anotherUserId = anotherUser.getId();

        assertThatThrownBy(() -> bookingService.cancelBooking(
                holdResponse.getBookingId(), anotherUserId))
                .isInstanceOf(com.eventbooking.common.exception.UnauthorizedActionException.class)
                .hasMessageContaining("your own bookings");
    }

    @Test
    void holdSeats_MultipleUsers_ConcurrentAttempts() {
        // Given - Two users trying to book overlapping seats
        User user2 = new User("user2@example.com", passwordEncoder.encode("password"),
                             "User 2", com.eventbooking.common.security.Role.USER);
        user2 = userRepository.save(user2);

        HoldRequest request1 = new HoldRequest(testShow.getId(), List.of(seat1.getId(), seat2.getId()));
        HoldRequest request2 = new HoldRequest(testShow.getId(), List.of(seat2.getId(), seat3.getId()));

        // When - First user holds successfully
        HoldResponse response1 = bookingService.holdSeats(request1, testUser.getId());
        assertThat(response1).isNotNull();

        // Then - Second user's request should fail (seat2 is already held)
        final Long user2Id = user2.getId();

        assertThatThrownBy(() -> bookingService.holdSeats(request2, user2Id))
                .isInstanceOf(SeatAlreadyHeldException.class);

        // Verify seat3 was NOT locked (all-or-nothing)
        assertThat(seatLockService.isLocked(testShow.getId(), seat3.getId())).isFalse();
    }

    @Test
    void getBookingById_Success() {
        // Given
        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        HoldResponse holdResponse = bookingService.holdSeats(request, testUser.getId());

        // When
        Booking booking = bookingService.getBookingById(holdResponse.getBookingId());

        // Then
        assertThat(booking.getId()).isEqualTo(holdResponse.getBookingId());
        assertThat(booking.getStatus()).isEqualTo("PENDING");
        assertThat(booking.getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    void getBookingsForUser_ReturnsUserBookings() {
        // Given - Create two bookings for the user
        HoldRequest request1 = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        HoldRequest request2 = new HoldRequest(testShow.getId(), List.of(seat2.getId()));
        bookingService.holdSeats(request1, testUser.getId());
        bookingService.holdSeats(request2, testUser.getId());

        // When
        List<Booking> bookings = bookingService.getBookingsForUser(testUser.getId());

        // Then
        assertThat(bookings).hasSize(2);
        assertThat(bookings).allMatch(b -> b.getUser().getId().equals(testUser.getId()));
    }

    private void clearAllLocks() {
        String pattern = "seat-lock:*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ------------------------------------------------------------------ confirm flow

    @Test
    void confirmBooking_Success_ConfirmsBookingAndMarksSeatsBooked() {
        // Force deterministic payment success
        paymentService.setSuccessRate(1.0);

        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId(), seat2.getId()));
        HoldResponse holdResponse = bookingService.holdSeats(request, testUser.getId());

        // When
        bookingService.confirmBooking(holdResponse.getBookingId(), testUser.getId());

        // Then
        Booking booking = bookingRepository.findById(holdResponse.getBookingId()).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo("CONFIRMED");
        assertThat(booking.getBookingSeats()).allMatch(bs -> bs.getActive());

        // Seats are now permanently BOOKED
        assertThat(seatRepository.findById(seat1.getId()).get().getStatus()).isEqualTo("BOOKED");
        assertThat(seatRepository.findById(seat2.getId()).get().getStatus()).isEqualTo("BOOKED");

        // Payment record exists and is SUCCESS
        Optional<Payment> payment = paymentRepository.findByBookingId(holdResponse.getBookingId());
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("SUCCESS");

        // Redis locks are released
        assertThat(seatLockService.isLocked(testShow.getId(), seat1.getId())).isFalse();
        assertThat(seatLockService.isLocked(testShow.getId(), seat2.getId())).isFalse();
    }

    @Test
    void confirmBooking_Failure_PaymentDeclined() {
        // Force deterministic payment failure
        paymentService.setSuccessRate(0.0);

        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        HoldResponse holdResponse = bookingService.holdSeats(request, testUser.getId());

        // When/Then
        assertThatThrownBy(() -> bookingService.confirmBooking(holdResponse.getBookingId(), testUser.getId()))
                .isInstanceOf(PaymentFailedException.class);

        // Booking should still be PENDING (transaction rolled back)
        Booking booking = bookingRepository.findById(holdResponse.getBookingId()).orElseThrow();
        assertThat(booking.getStatus()).isEqualTo("CANCELLED");

        // Locks should be released
        assertThat(seatLockService.isLocked(testShow.getId(), seat1.getId())).isFalse();
    }

    @Test
    void confirmBooking_Failure_WhenNotOwner() {
        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        HoldResponse holdResponse = bookingService.holdSeats(request, testUser.getId());

        User anotherUser = new User("another@example.com", passwordEncoder.encode("password"),
                                   "Another User", com.eventbooking.common.security.Role.USER);
        anotherUser = userRepository.save(anotherUser);

        final Long anotherUserId = anotherUser.getId();
        assertThatThrownBy(() -> bookingService.confirmBooking(holdResponse.getBookingId(), anotherUserId))
                .isInstanceOf(com.eventbooking.common.exception.UnauthorizedActionException.class);
    }

    @Test
    void confirmBooking_Failure_WhenAlreadyConfirmed() {
        paymentService.setSuccessRate(1.0);

        HoldRequest request = new HoldRequest(testShow.getId(), List.of(seat1.getId()));
        HoldResponse holdResponse = bookingService.holdSeats(request, testUser.getId());
        bookingService.confirmBooking(holdResponse.getBookingId(), testUser.getId());

        // Second confirm should fail
        assertThatThrownBy(() -> bookingService.confirmBooking(holdResponse.getBookingId(), testUser.getId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmBooking_Failure_WhenExpired() {
        // Create an expired booking manually
        Booking expiredBooking = new Booking(testUser, testShow, new BigDecimal("100.00"),
                Instant.now().minus(Duration.ofMinutes(1)));
        expiredBooking = bookingRepository.save(expiredBooking);
        expiredBooking.addBookingSeat(
                new BookingSeat(expiredBooking, seat1, new BigDecimal("100.00"))
        );
        bookingRepository.save(expiredBooking);

        final UUID expiredBookingId = expiredBooking.getId();

        assertThatThrownBy(() ->
                bookingService.confirmBooking(expiredBookingId, testUser.getId()))
                .isInstanceOf(BookingExpiredException.class);

    }
}
