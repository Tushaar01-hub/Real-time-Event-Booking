package com.eventbooking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingSummaryDto(
        UUID id,
        String showTitle,
        String status,
        BigDecimal totalAmount,
        Instant createdAt) {
}
