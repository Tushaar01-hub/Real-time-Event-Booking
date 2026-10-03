package com.eventbooking.catalog.seat;

import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Seat operations.
 * Public endpoint for seat map, admin-only for seat modifications.
 */
@RestController
@RequestMapping("/api")
public class SeatController {

    private final SeatService seatService;
    private final SeatMapService seatMapService;

    public SeatController(SeatService seatService, SeatMapService seatMapService) {
        this.seatService = seatService;
        this.seatMapService = seatMapService;
    }

    /**
     * Get seat map for a show with Redis lock overlay (public).
     * Optionally provide bookingId to see your own held seats.
     */
    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<SeatDto>> getSeatMap(
            @PathVariable Long showId,
            @RequestParam(required = false) UUID bookingId) {
        List<SeatDto> seatMap = seatMapService.getSeatMapWithLocks(showId, bookingId);
        return ResponseEntity.ok(seatMap);
    }

    /**
     * Get a single seat by ID (public).
     */
    @GetMapping("/seats/{id}")
    public ResponseEntity<SeatDto> getSeatById(@PathVariable Long id) {
        SeatDto seat = seatService.getSeatById(id);
        return ResponseEntity.ok(seat);
    }

    /**
     * Update seat status - block or unblock (admin only).
     * Request body: { "status": "AVAILABLE" or "BLOCKED" }
     */
    @PatchMapping("/shows/{showId}/seats/{seatId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeatDto> updateSeatStatus(
            @PathVariable Long showId,
            @PathVariable Long seatId,
            @RequestBody UpdateSeatRequest request) {

        if (request.getStatus() != null) {
            SeatDto updated = seatService.updateSeatStatus(seatId, request.getStatus());
            return ResponseEntity.ok(updated);
        } else if (request.getPrice() != null) {
            SeatDto updated = seatService.updateSeatPrice(seatId, request.getPrice());
            return ResponseEntity.ok(updated);
        } else {
            throw new IllegalArgumentException("Must provide either status or price to update");
        }
    }

    /**
     * Request DTO for seat updates.
     */
    public static class UpdateSeatRequest {
        private String status;

        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal price;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}
