package com.eventbooking.booking.lock;

import com.eventbooking.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SeatLockService.
 * Tests Redis locking behavior with real Redis instance.
 */
class SeatLockServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Long TEST_SHOW_ID = 100L;
    private static final Long SEAT_1 = 1L;
    private static final Long SEAT_2 = 2L;
    private static final Long SEAT_3 = 3L;

    @BeforeEach
    void setUp() {
        // Clean up any existing test locks
        clearTestLocks();
    }

    @AfterEach
    void tearDown() {
        clearTestLocks();
    }

    @Test
    void acquireLocks_Success_WhenSeatsAreAvailable() {
        // Given
        UUID bookingId = UUID.randomUUID();
        List<Long> seatIds = List.of(SEAT_1, SEAT_2);

        // When
        SeatLockService.LockResult result = seatLockService.acquireLocks(TEST_SHOW_ID, seatIds, bookingId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(seatLockService.isLocked(TEST_SHOW_ID, SEAT_1)).isTrue();
        assertThat(seatLockService.isLocked(TEST_SHOW_ID, SEAT_2)).isTrue();
        assertThat(seatLockService.getLockOwner(TEST_SHOW_ID, SEAT_1)).isEqualTo(bookingId);
        assertThat(seatLockService.getLockOwner(TEST_SHOW_ID, SEAT_2)).isEqualTo(bookingId);
    }

    @Test
    void acquireLocks_Failure_WhenOneSeatIsAlreadyLocked() {
        // Given
        UUID firstBooking = UUID.randomUUID();
        UUID secondBooking = UUID.randomUUID();

        // First booking locks SEAT_1
        seatLockService.acquireLocks(TEST_SHOW_ID, List.of(SEAT_1), firstBooking);

        // When - Second booking tries to lock SEAT_1 and SEAT_2
        SeatLockService.LockResult result = seatLockService.acquireLocks(
                TEST_SHOW_ID,
                List.of(SEAT_1, SEAT_2),
                secondBooking
        );

        // Then - All-or-nothing: SEAT_2 should NOT be locked either
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).contains("Seat is already held");

        // SEAT_1 still locked by first booking
        assertThat(seatLockService.getLockOwner(TEST_SHOW_ID, SEAT_1)).isEqualTo(firstBooking);

        // SEAT_2 should NOT be locked (atomic failure)
        assertThat(seatLockService.isLocked(TEST_SHOW_ID, SEAT_2)).isFalse();
    }

    @Test
    void releaseLocks_Success_WhenOwnerMatchesAndLocksExist() {
        // Given
        UUID bookingId = UUID.randomUUID();
        List<Long> seatIds = List.of(SEAT_1, SEAT_2);
        seatLockService.acquireLocks(TEST_SHOW_ID, seatIds, bookingId);

        // When
        int released = seatLockService.releaseLocks(TEST_SHOW_ID, seatIds, bookingId);

        // Then
        assertThat(released).isEqualTo(2);
        assertThat(seatLockService.isLocked(TEST_SHOW_ID, SEAT_1)).isFalse();
        assertThat(seatLockService.isLocked(TEST_SHOW_ID, SEAT_2)).isFalse();
    }

    @Test
    void releaseLocks_Failure_WhenWrongOwnerTries() {
        // Given
        UUID correctOwner = UUID.randomUUID();
        UUID wrongOwner = UUID.randomUUID();
        List<Long> seatIds = List.of(SEAT_1);
        seatLockService.acquireLocks(TEST_SHOW_ID, seatIds, correctOwner);

        // When - Wrong owner tries to release
        int released = seatLockService.releaseLocks(TEST_SHOW_ID, seatIds, wrongOwner);

        // Then - Lock should NOT be released
        assertThat(released).isEqualTo(0);
        assertThat(seatLockService.isLocked(TEST_SHOW_ID, SEAT_1)).isTrue();
        assertThat(seatLockService.getLockOwner(TEST_SHOW_ID, SEAT_1)).isEqualTo(correctOwner);
    }

    @Test
    void lockExpiry_LocksExpireAfterTTL() throws InterruptedException {
        // Given
        UUID bookingId = UUID.randomUUID();
        seatLockService.acquireLocks(TEST_SHOW_ID, List.of(SEAT_1), bookingId);

        // When - Check TTL is set
        Long ttl = seatLockService.getLockTTL(TEST_SHOW_ID, SEAT_1);

        // Then
        assertThat(ttl).isNotNull();
        assertThat(ttl).isGreaterThan(0L).isLessThanOrEqualTo(300L);

        // Note: Full TTL expiry test would take 5 minutes, so we just verify TTL is set
        // In a real scenario, you could use a shorter TTL for testing
    }

    @Test
    void getLocksForBooking_ReturnsCorrectSeats() {
        // Given
        UUID bookingId = UUID.randomUUID();
        List<Long> seatIds = List.of(SEAT_1, SEAT_2, SEAT_3);
        seatLockService.acquireLocks(TEST_SHOW_ID, seatIds, bookingId);

        // When
        List<Long> lockedSeats = seatLockService.getLocksForBooking(TEST_SHOW_ID, bookingId);

        // Then
        assertThat(lockedSeats).containsExactlyInAnyOrderElementsOf(seatIds);
    }

    @Test
    void getLocksForBooking_ReturnsEmpty_WhenNoLocksExist() {
        // Given
        UUID bookingId = UUID.randomUUID();

        // When
        List<Long> lockedSeats = seatLockService.getLocksForBooking(TEST_SHOW_ID, bookingId);

        // Then
        assertThat(lockedSeats).isEmpty();
    }

    @Test
    void acquireLocks_CanReacquire_AfterRelease() {
        // Given
        UUID firstBooking = UUID.randomUUID();
        UUID secondBooking = UUID.randomUUID();
        List<Long> seatIds = List.of(SEAT_1);

        // First booking acquires and releases
        seatLockService.acquireLocks(TEST_SHOW_ID, seatIds, firstBooking);
        seatLockService.releaseLocks(TEST_SHOW_ID, seatIds, firstBooking);

        // When - Second booking tries to acquire the same seat
        SeatLockService.LockResult result = seatLockService.acquireLocks(TEST_SHOW_ID, seatIds, secondBooking);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(seatLockService.getLockOwner(TEST_SHOW_ID, SEAT_1)).isEqualTo(secondBooking);
    }

    @Test
    void acquireLocks_MultipleShows_IndependentLocks() {
        // Given
        Long show1 = 100L;
        Long show2 = 200L;
        UUID booking1 = UUID.randomUUID();
        UUID booking2 = UUID.randomUUID();

        // When - Same seat ID in different shows
        SeatLockService.LockResult result1 = seatLockService.acquireLocks(show1, List.of(SEAT_1), booking1);
        SeatLockService.LockResult result2 = seatLockService.acquireLocks(show2, List.of(SEAT_1), booking2);

        // Then - Both should succeed (different shows)
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();
        assertThat(seatLockService.getLockOwner(show1, SEAT_1)).isEqualTo(booking1);
        assertThat(seatLockService.getLockOwner(show2, SEAT_1)).isEqualTo(booking2);

        // Cleanup
        seatLockService.releaseLocks(show2, List.of(SEAT_1), booking2);
    }

    @Test
    void acquireLocks_EmptyList_ReturnsFailure() {
        // Given
        UUID bookingId = UUID.randomUUID();
        List<Long> emptyList = List.of();

        // When
        SeatLockService.LockResult result = seatLockService.acquireLocks(TEST_SHOW_ID, emptyList, bookingId);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailureReason()).contains("No seats provided");
    }

    private void clearTestLocks() {
        String pattern = "seat-lock:" + TEST_SHOW_ID + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        // Also clear show 200 from the multi-show test
        String pattern2 = "seat-lock:200:*";
        var keys2 = redisTemplate.keys(pattern2);
        if (keys2 != null && !keys2.isEmpty()) {
            redisTemplate.delete(keys2);
        }
    }
}
