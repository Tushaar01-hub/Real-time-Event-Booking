package com.eventbooking.catalog.seat;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for Seat information in the seat map.
 * Includes derived HELD status from Redis.
 */
public class SeatDto {

    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private String section;
    private BigDecimal price;
    private String status;  // AVAILABLE | BOOKED | BLOCKED | HELD | UNAVAILABLE

    // Only present if this seat is held by the current user
    private Instant holdExpiresAt;

    // ---------------------------------------------------------------- constructors

    public SeatDto() {
    }

    public SeatDto(Long id, String rowLabel, Integer seatNumber, String section,
                   BigDecimal price, String status) {
        this.id = id;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.section = section;
        this.price = price;
        this.status = status;
    }

    // ---------------------------------------------------------------- getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public void setRowLabel(String rowLabel) {
        this.rowLabel = rowLabel;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(Instant holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }
}
