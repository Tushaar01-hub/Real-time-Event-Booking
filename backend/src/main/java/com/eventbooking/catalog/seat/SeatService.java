package com.eventbooking.catalog.seat;

import com.eventbooking.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service layer for Seat operations.
 * Handles CRUD operations on seats.
 */
@Service
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    public SeatService(SeatRepository seatRepository, SeatMapper seatMapper) {
        this.seatRepository = seatRepository;
        this.seatMapper = seatMapper;
    }

    /**
     * Get all seats for a show (without Redis lock overlay).
     * For seat map with lock status, use SeatMapService instead.
     */
    public List<SeatDto> getSeatsByShowId(Long showId) {
        List<Seat> seats = seatRepository.findByShowIdOrderBySectionAndPosition(showId);
        return seats.stream()
                .map(seatMapper::toDto)
                .toList();
    }

    /**
     * Get a single seat by ID.
     */
    public SeatDto getSeatById(Long id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));
        return seatMapper.toDto(seat);
    }

    /**
     * Block or unblock a seat (admin only).
     * Cannot block a seat that is already booked.
     */
    @Transactional
    public SeatDto updateSeatStatus(Long id, String newStatus) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));

        // Validate status
        if (!List.of("AVAILABLE", "BLOCKED").contains(newStatus)) {
            throw new IllegalArgumentException("Can only set status to AVAILABLE or BLOCKED. BOOKED is managed by the booking system.");
        }

        // Cannot change status of a booked seat
        if ("BOOKED".equals(seat.getStatus())) {
            throw new IllegalArgumentException("Cannot change status of a booked seat");
        }

        seat.setStatus(newStatus);
        Seat updated = seatRepository.save(seat);
        return seatMapper.toDto(updated);
    }

    /**
     * Update seat price (admin only).
     * Cannot change price of a booked seat.
     */
    @Transactional
    public SeatDto updateSeatPrice(Long id, BigDecimal newPrice) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", id));

        if ("BOOKED".equals(seat.getStatus())) {
            throw new IllegalArgumentException("Cannot change price of a booked seat");
        }

        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        seat.setPrice(newPrice);
        Seat updated = seatRepository.save(seat);
        return seatMapper.toDto(updated);
    }

    /**
     * Validate that all seats belong to the specified show and are available.
     * Used by booking service before attempting to hold seats.
     */
    public boolean areSeatsAvailableForShow(List<Long> seatIds, Long showId) {
        if (seatIds == null || seatIds.isEmpty()) {
            return false;
        }
        return seatRepository.areAllSeatsAvailableForShow(seatIds, showId, seatIds.size());
    }
}
