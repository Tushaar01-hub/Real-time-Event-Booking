package com.eventbooking.booking.controller;

import com.eventbooking.booking.dto.HoldRequest;
import com.eventbooking.booking.dto.HoldResponse;
import com.eventbooking.booking.service.BookingService;
import com.eventbooking.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for booking operations.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Hold seats for a booking.
     * Creates a PENDING booking and acquires Redis locks on the seats.
     *
     * POST /api/bookings/hold
     * Requires authentication.
     *
     * @param request Hold request with showId and seatIds
     * @param user    Current authenticated user
     * @return HoldResponse with booking ID and expiry time
     */
    @PostMapping("/hold")
    public ResponseEntity<HoldResponse> holdSeats(
            @Valid @RequestBody HoldRequest request,
            @AuthenticationPrincipal User user) {

        HoldResponse response = bookingService.holdSeats(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cancel a booking.
     * Releases Redis locks and marks the booking as CANCELLED.
     *
     * POST /api/bookings/{id}/cancel
     * Requires authentication and ownership.
     *
     * @param bookingId Booking UUID
     * @param user      Current authenticated user
     * @return 204 No Content on success
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable("id") String bookingId,
            @AuthenticationPrincipal User user) {

        bookingService.cancelBooking(java.util.UUID.fromString(bookingId), user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirm a booking after successful payment.
     * POST /api/bookings/{id}/confirm
     * Requires authentication and ownership.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmBooking(
            @PathVariable("id") String bookingId,
            @AuthenticationPrincipal User user) {

        bookingService.confirmBooking(java.util.UUID.fromString(bookingId), user.getId());
        return ResponseEntity.noContent().build();
    }
}
