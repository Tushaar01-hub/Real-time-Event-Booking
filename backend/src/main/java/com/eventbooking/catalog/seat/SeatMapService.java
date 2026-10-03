package com.eventbooking.catalog.seat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for generating seat maps with Redis lock overlay.
 * Overlays HELD status from Redis locks onto database seat status.
 */
@Service
public class SeatMapService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;
    private final StringRedisTemplate redisTemplate;

    private static final String SEAT_LOCK_PREFIX = "seat-lock:";
    private static final long HOLD_DURATION_SECONDS = 300; // 5 minutes

    public SeatMapService(SeatRepository seatRepository,
                         SeatMapper seatMapper,
                         StringRedisTemplate redisTemplate) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Get seat map for a show with Redis lock overlay.
     * Seats that are locked in Redis show as HELD (unless locked by current user's booking).
     *
     * @param showId The show ID
     * @param currentUserBookingId The current user's booking ID (if they have seats held)
     * @return List of seats with accurate status including HELD
     */
    public List<SeatDto> getSeatMapWithLocks(Long showId, UUID currentUserBookingId) {
        // Get all seats from database
        List<Seat> seats = seatRepository.findByShowIdOrderBySectionAndPosition(showId);

        // Convert to DTOs
        List<SeatDto> seatDtos = seats.stream()
                .map(seatMapper::toDto)
                .collect(Collectors.toList());

        // Get all seat IDs
        List<Long> seatIds = seats.stream()
                .map(Seat::getId)
                .collect(Collectors.toList());

        // Check Redis locks for all seats in one batch operation
        if (!seatIds.isEmpty()) {
            List<String> lockKeys = seatIds.stream()
                    .map(seatId -> buildLockKey(showId, seatId))
                    .collect(Collectors.toList());

            // Use pipeline to get all locks at once
            List<String> lockValues = redisTemplate.opsForValue().multiGet(lockKeys);

            // Overlay lock status
            for (int i = 0; i < seatDtos.size(); i++) {
                SeatDto seat = seatDtos.get(i);
                String lockValue = lockValues != null && i < lockValues.size() ? lockValues.get(i) : null;

                if (lockValue != null) {
                    // Seat is locked in Redis
                    UUID lockOwner = UUID.fromString(lockValue);

                    if (currentUserBookingId != null && lockOwner.equals(currentUserBookingId)) {
                        // Current user holds this seat - show as HELD with expiry
                        seat.setStatus("HELD");

                        // Get TTL for this lock
                        Long ttl = redisTemplate.getExpire(lockKeys.get(i), TimeUnit.SECONDS);
                        if (ttl != null && ttl > 0) {
                            seat.setHoldExpiresAt(Instant.now().plusSeconds(ttl));
                        }
                    } else if ("AVAILABLE".equals(seat.getStatus())) {
                        // Someone else holds this seat - show as UNAVAILABLE
                        seat.setStatus("UNAVAILABLE");
                    }
                }
            }
        }

        return seatDtos;
    }

    /**
     * Build Redis lock key for a seat.
     */
    private String buildLockKey(Long showId, Long seatId) {
        return SEAT_LOCK_PREFIX + showId + ":" + seatId;
    }
}
