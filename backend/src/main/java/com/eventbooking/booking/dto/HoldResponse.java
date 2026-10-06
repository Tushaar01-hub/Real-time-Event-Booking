package com.eventbooking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response after successfully holding seats.
 */
public class HoldResponse {

    private UUID bookingId;
    private Long showId;
    private List<Long> seatIds;
    private BigDecimal totalAmount;
    private Instant expiresAt;
    private String status;

    // ---------------------------------------------------------------- constructors

    public HoldResponse() {
    }

    public HoldResponse(UUID bookingId, Long showId, List<Long> seatIds,
                        BigDecimal totalAmount, Instant expiresAt, String status) {
        this.bookingId = bookingId;
        this.showId = showId;
        this.seatIds = seatIds;
        this.totalAmount = totalAmount;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    // ---------------------------------------------------------------- getters/setters

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public Long getShowId() {
        return showId;
    }

    public void setShowId(Long showId) {
        this.showId = showId;
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }

    public void setSeatIds(List<Long> seatIds) {
        this.seatIds = seatIds;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
