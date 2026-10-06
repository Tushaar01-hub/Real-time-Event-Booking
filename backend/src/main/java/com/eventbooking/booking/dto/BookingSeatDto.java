package com.eventbooking.booking.dto;

import java.math.BigDecimal;

/**
 * DTO for a seat in a booking.
 */
public class BookingSeatDto {

    private Long seatId;
    private String section;
    private String rowLabel;
    private Integer seatNumber;
    private BigDecimal priceAtBooking;
    private Boolean active;

    // ---------------------------------------------------------------- constructors

    public BookingSeatDto() {
    }

    public BookingSeatDto(Long seatId, String section, String rowLabel,
                          Integer seatNumber, BigDecimal priceAtBooking, Boolean active) {
        this.seatId = seatId;
        this.section = section;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.priceAtBooking = priceAtBooking;
        this.active = active;
    }

    // ---------------------------------------------------------------- getters/setters

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
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

    public BigDecimal getPriceAtBooking() {
        return priceAtBooking;
    }

    public void setPriceAtBooking(BigDecimal priceAtBooking) {
        this.priceAtBooking = priceAtBooking;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
