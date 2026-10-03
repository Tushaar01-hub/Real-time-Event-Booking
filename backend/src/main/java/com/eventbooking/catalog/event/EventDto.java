package com.eventbooking.catalog.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Data Transfer Object for Event.
 * Used for both requests and responses.
 */
public class EventDto {

    private Long id;

    @NotBlank(message = "Event title is required")
    @Size(max = 200, message = "Event title must not exceed 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private String categoryName;  // read-only, for responses

    @NotBlank(message = "Location is required")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Size(max = 500, message = "Poster URL must not exceed 500 characters")
    private String posterUrl;

    private Instant createdAt;  // read-only
    private Instant updatedAt;  // read-only

    // ---------------------------------------------------------------- constructors

    public EventDto() {
    }

    public EventDto(Long id, String title, String description, Long categoryId, String categoryName,
                    String location, String posterUrl, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.location = location;
        this.posterUrl = posterUrl;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
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
