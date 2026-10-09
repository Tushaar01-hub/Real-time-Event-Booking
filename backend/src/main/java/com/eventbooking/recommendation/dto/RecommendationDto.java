package com.eventbooking.recommendation.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for event recommendations.
 */
public record RecommendationDto(
        Long eventId,
        String title,
        String categoryName,
        String location,
        Instant startTime,
        BigDecimal minPrice,
        String posterUrl,
        String reason,
        double score) {
}