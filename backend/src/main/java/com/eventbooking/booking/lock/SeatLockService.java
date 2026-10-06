package com.eventbooking.booking.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing Redis-based seat locks.
 * Uses Lua scripts for atomic operations.
 */
@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);
    private static final String SEAT_LOCK_PREFIX = "seat-lock:";
    private static final long HOLD_DURATION_SECONDS = 300; // 5 minutes

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<String> acquireScript;
    private final DefaultRedisScript<Long> releaseScript;

    public SeatLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // Load acquire_locks.lua
        this.acquireScript = new DefaultRedisScript<>();
        this.acquireScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/acquire_locks.lua"))
        );
        this.acquireScript.setResultType(String.class);

        // Load release_lock.lua
        this.releaseScript = new DefaultRedisScript<>();
        this.releaseScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/release_lock.lua"))
        );
        this.releaseScript.setResultType(Long.class);
    }

    /**
     * Atomically acquire locks on multiple seats for a booking.
     * All-or-nothing: if any seat is already locked, no locks are acquired.
     *
     * @param showId    Show ID
     * @param seatIds   Seat IDs to lock
     * @param bookingId Booking ID that will own the locks
     * @return LockResult indicating success or the first conflicting seat
     */
    public LockResult acquireLocks(Long showId, List<Long> seatIds, UUID bookingId) {
        if (seatIds == null || seatIds.isEmpty()) {
            return LockResult.failure("No seats provided");
        }

        // Build lock keys
        List<String> keys = seatIds.stream()
                .map(seatId -> buildLockKey(showId, seatId))
                .collect(Collectors.toList());

        // Execute Lua script
        String result = redisTemplate.execute(
                acquireScript,
                keys,
                bookingId.toString(),
                String.valueOf(HOLD_DURATION_SECONDS)
        );

        if ("OK".equals(result)) {
            log.debug("Acquired locks for booking {} on {} seats", bookingId, seatIds.size());
            return LockResult.success();
        } else if (result != null && result.startsWith("LOCKED:")) {
            String lockedKey = result.substring("LOCKED:".length());
            log.debug("Failed to acquire locks for booking {}: {} is already locked", bookingId, lockedKey);
            return LockResult.failure("Seat is already held by another user", lockedKey);
        } else {
            log.error("Unexpected result from acquire script: {}", result);
            return LockResult.failure("Unexpected lock acquisition failure");
        }
    }

    /**
     * Release locks for specific seats owned by a booking.
     * Uses compare-and-delete to ensure we only release our own locks.
     *
     * @param showId    Show ID
     * @param seatIds   Seat IDs to unlock
     * @param bookingId Booking ID that owns the locks
     * @return Number of locks successfully released
     */
    public int releaseLocks(Long showId, List<Long> seatIds, UUID bookingId) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }

        int released = 0;
        for (Long seatId : seatIds) {
            String key = buildLockKey(showId, seatId);
            Long result = redisTemplate.execute(
                    releaseScript,
                    List.of(key),
                    bookingId.toString()
            );

            if (result != null && result == 1) {
                released++;
            }
        }

        log.debug("Released {} locks for booking {}", released, bookingId);
        return released;
    }

    /**
     * Check if a specific seat is currently locked.
     *
     * @param showId Show ID
     * @param seatId Seat ID
     * @return true if the seat is locked
     */
    public boolean isLocked(Long showId, Long seatId) {
        String key = buildLockKey(showId, seatId);
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Get the booking ID that currently holds a lock on a seat.
     *
     * @param showId Show ID
     * @param seatId Seat ID
     * @return Booking UUID or null if not locked
     */
    public UUID getLockOwner(Long showId, Long seatId) {
        String key = buildLockKey(showId, seatId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? UUID.fromString(value) : null;
    }

    /**
     * Get all seat IDs currently locked by a specific booking.
     *
     * @param showId    Show ID
     * @param bookingId Booking ID
     * @return List of seat IDs locked by this booking
     */
    public List<Long> getLocksForBooking(Long showId, UUID bookingId) {
        // Note: This is not super efficient for large shows.
        // For production, consider maintaining a reverse index.
        String pattern = buildLockKey(showId, null) + "*";
        var keys = redisTemplate.keys(pattern);

        if (keys == null || keys.isEmpty()) {
            return List.of();
        }

        return keys.stream()
                .filter(key -> {
                    String value = redisTemplate.opsForValue().get(key);
                    return bookingId.toString().equals(value);
                })
                .map(this::extractSeatIdFromKey)
                .collect(Collectors.toList());
    }

    /**
     * Get the remaining TTL for a lock in seconds.
     *
     * @param showId Show ID
     * @param seatId Seat ID
     * @return TTL in seconds, or null if not locked
     */
    public Long getLockTTL(Long showId, Long seatId) {
        String key = buildLockKey(showId, seatId);
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * Build Redis lock key for a seat.
     */
    private String buildLockKey(Long showId, Long seatId) {
        if (seatId == null) {
            return SEAT_LOCK_PREFIX + showId + ":";
        }
        return SEAT_LOCK_PREFIX + showId + ":" + seatId;
    }

    /**
     * Extract seat ID from a lock key.
     */
    private Long extractSeatIdFromKey(String key) {
        String[] parts = key.split(":");
        return Long.parseLong(parts[parts.length - 1]);
    }

    /**
     * Result of a lock acquisition attempt.
     */
    public static class LockResult {
        private final boolean success;
        private final String failureReason;
        private final String conflictingKey;

        private LockResult(boolean success, String failureReason, String conflictingKey) {
            this.success = success;
            this.failureReason = failureReason;
            this.conflictingKey = conflictingKey;
        }

        public static LockResult success() {
            return new LockResult(true, null, null);
        }

        public static LockResult failure(String reason) {
            return new LockResult(false, reason, null);
        }

        public static LockResult failure(String reason, String conflictingKey) {
            return new LockResult(false, reason, conflictingKey);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public String getConflictingKey() {
            return conflictingKey;
        }
    }
}
