package com.eventbooking.catalog.show;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Data Transfer Object for Show.
 * Used for responses and updates.
 */
public class ShowDto {

    private Long id;

    @NotNull(message = "Event ID is required")
    private Long eventId;

    private String eventTitle;  // read-only, for responses

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @NotBlank(message = "Venue is required")
    @Size(max = 255, message = "Venue must not exceed 255 characters")
    private String venue;

    @NotBlank(message = "Status is required")
    @Size(max = 20)
    private String status;  // SCHEDULED | CANCELLED

    private Instant createdAt;  // read-only
    private Instant updatedAt;  // read-only

    // ---------------------------------------------------------------- constructors

    public ShowDto() {
    }

    public ShowDto(Long id, Long eventId, String eventTitle, Instant startTime, Instant endTime,
                   String venue, String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.venue = venue;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ---------------------------------------------------------------- getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
