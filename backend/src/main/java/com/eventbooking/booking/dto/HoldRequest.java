package com.eventbooking.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request to hold seats for a booking.
 */
public class HoldRequest {

    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 6, message = "Cannot hold more than 6 seats at once")
    private List<Long> seatIds;

    // ---------------------------------------------------------------- constructors

    public HoldRequest() {
    }

    public HoldRequest(Long showId, List<Long> seatIds) {
        this.showId = showId;
        this.seatIds = seatIds;
    }

    // ---------------------------------------------------------------- getters/setters

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
}
