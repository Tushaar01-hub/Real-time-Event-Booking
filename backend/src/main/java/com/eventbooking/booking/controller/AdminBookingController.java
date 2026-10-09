package com.eventbooking.booking.controller;

import com.eventbooking.booking.dto.BookingDetailDto;
import com.eventbooking.booking.dto.BookingSummaryDto;
import com.eventbooking.booking.service.BookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * Admin REST controller for booking operations.
 */
@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * List all bookings for admin.
     */
    @GetMapping
    public ResponseEntity<Page<BookingSummaryDto>> getAllBookings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long showId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Sort.Direction direction = sort.length > 1 && sort[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        String sortField = sort[0];

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<BookingSummaryDto> bookings = bookingService.getAllBookings(status, showId, pageable);

        return ResponseEntity.ok(bookings);
    }

    /**
     * Get booking details for admin.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingDetailDto> getBookingDetails(@PathVariable String id) {
        // Admin can view any booking. Use a dummy userId that is unlikely to match ownership
        // to bypass the ownership check if it's strict, or consider refactoring the check.
        // Given the current implementation in BookingService.getBookingDetails checks ownership:
        // Admin needs a service method that skips ownership or uses an admin ID.
        // For now, let's implement a service method specifically for admin detail.
        return ResponseEntity.ok(bookingService.getAdminBookingDetails(UUID.fromString(id)));
    }
}
