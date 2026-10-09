package com.eventbooking.catalog.event;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Event operations.
 * Public endpoints for reading, admin-only for modifications.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Search and filter events with pagination (public).
     * Query params:
     * - q: search query (title)
     * - category: category ID filter
     * - location: location filter (contains)
     * - page: page number (0-indexed, default 0)
     * - size: page size (default 20)
     * - sort: sort field and direction (e.g., "title,asc" or "createdAt,desc")
     */
    @GetMapping
    public ResponseEntity<Page<EventDto>> searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        // Parse sort parameter
        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortField = sort[0];

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<EventDto> events = eventService.searchEvents(q, category, location, pageable);

        return ResponseEntity.ok(events);
    }

    /**
     * Get a single event by ID (public).
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getEventById(@PathVariable Long id) {
        EventDto event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Create a new event (admin only).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventDto dto) {
        EventDto created = eventService.createEvent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing event (admin only).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventDto dto) {
        EventDto updated = eventService.updateEvent(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an event (admin only).
     * Will fail with 409 if shows with confirmed bookings exist.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
