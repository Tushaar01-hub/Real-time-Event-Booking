package com.eventbooking.catalog.seat;

import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Seat entity and SeatDto.
 */
@Component
public class SeatMapper {

    /**
     * Convert entity to DTO.
     * Redis lock status is overlaid separately by SeatMapService.
     */
    public SeatDto toDto(Seat seat) {
        if (seat == null) {
            return null;
        }
        return new SeatDto(
            seat.getId(),
            seat.getRowLabel(),
            seat.getSeatNumber(),
            seat.getSection(),
            seat.getPrice(),
            seat.getStatus()
        );
    }
}
