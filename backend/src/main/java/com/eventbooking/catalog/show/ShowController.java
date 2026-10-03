package com.eventbooking.catalog.show;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Show operations.
 * Public endpoints for reading, admin-only for modifications.
 */
@RestController
@RequestMapping("/api")
public class ShowController {

    private final ShowService showService;

    public ShowController(ShowService showService) {
        this.showService = showService;
    }

    /**
     * Get all shows for a specific event (public).
     */
    @GetMapping("/events/{eventId}/shows")
    public ResponseEntity<List<ShowDto>> getShowsByEventId(@PathVariable Long eventId) {
        List<ShowDto> shows = showService.getShowsByEventId(eventId);
        return ResponseEntity.ok(shows);
    }

    /**
     * Get a single show by ID (public).
     */
    @GetMapping("/shows/{id}")
    public ResponseEntity<ShowDto> getShowById(@PathVariable Long id) {
        ShowDto show = showService.getShowById(id);
        return ResponseEntity.ok(show);
    }

    /**
     * Create a new show with seat layout (admin only).
     */
    @PostMapping("/events/{eventId}/shows")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDto> createShow(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateShowRequest request) {
        // Ensure eventId in path matches the one in request body
        if (!eventId.equals(request.getEventId())) {
            throw new IllegalArgumentException("Event ID in path must match event ID in request body");
        }
        ShowDto created = showService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing show (admin only).
     * Layout cannot be modified once created.
     */
    @PutMapping("/shows/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ShowDto> updateShow(
            @PathVariable Long id,
            @Valid @RequestBody ShowDto dto) {
        ShowDto updated = showService.updateShow(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a show (admin only).
     * Will fail if confirmed bookings exist.
     */
    @DeleteMapping("/shows/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
