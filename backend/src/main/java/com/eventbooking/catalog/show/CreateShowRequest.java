package com.eventbooking.catalog.show;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Data Transfer Object for Show creation requests.
 * Includes seat layout specification.
 */
public class CreateShowRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @NotBlank(message = "Venue is required")
    @Size(max = 255, message = "Venue must not exceed 255 characters")
    private String venue;

    @NotEmpty(message = "Seat layout must contain at least one section")
    @Valid
    private List<SeatLayoutDto> seatLayout;

    // ---------------------------------------------------------------- constructors

    public CreateShowRequest() {
    }

    public CreateShowRequest(Long eventId, Instant startTime, Instant endTime,
                           String venue, List<SeatLayoutDto> seatLayout) {
        this.eventId = eventId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = venue;
        this.seatLayout = seatLayout;
    }

    // ---------------------------------------------------------------- getters/setters

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public List<SeatLayoutDto> getSeatLayout() {
        return seatLayout;
    }

    public void setSeatLayout(List<SeatLayoutDto> seatLayout) {
        this.seatLayout = seatLayout;
    }
}
