package com.eventbooking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for booking details.
 */
public class BookingDto {

    private UUID id;
    private Long userId;
    private Long showId;
    private String showTitle;
    private String status;
    private BigDecimal totalAmount;
    private Instant expiresAt;
    private Instant createdAt;
    private List<BookingSeatDto> seats;

    // ---------------------------------------------------------------- constructors

    public BookingDto() {
    }

    // ---------------------------------------------------------------- getters/setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public String getShowTitle() {
        return showTitle;
    }

    public void setShowTitle(String showTitle) {
        this.showTitle = showTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<BookingSeatDto> getSeats() {
        return seats;
    }

    public void setSeats(List<BookingSeatDto> seats) {
        this.seats = seats;
    }
}
