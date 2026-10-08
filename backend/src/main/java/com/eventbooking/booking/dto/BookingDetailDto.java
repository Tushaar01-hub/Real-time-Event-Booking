package com.eventbooking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingDetailDto(
        UUID id,
        String showTitle,
        String status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant expiresAt,
        String paymentStatus,
        List<BookingSeatDto> seats) {
}
