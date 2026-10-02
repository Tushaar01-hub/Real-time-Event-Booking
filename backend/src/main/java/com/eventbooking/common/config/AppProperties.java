package com.eventbooking.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed business configuration. Values live in application.yml under "app.*"
 * so nothing is hard-coded in service classes.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Booking booking) {

    /**
     * @param holdTtl            how long a seat stays held in Redis (seat-lock TTL)
     * @param maxSeatsPerBooking upper bound on seats in one hold request
     * @param cancellationCutoff confirmed bookings can only be cancelled this long before the show
     */
    public record Booking(Duration holdTtl, int maxSeatsPerBooking, Duration cancellationCutoff) {
    }
}
