package com.eventbooking.catalog.show;

import com.eventbooking.catalog.event.Event;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Show entity and ShowDto.
 * Keeps entities isolated from the API layer.
 */
@Component
public class ShowMapper {

    /**
     * Convert entity to DTO.
     */
    public ShowDto toDto(Show show) {
        if (show == null) {
            return null;
        }
        return new ShowDto(
            show.getId(),
            show.getEvent().getId(),
            show.getEvent().getTitle(),
            show.getStartTime(),
            show.getEndTime(),
            show.getVenue(),
            show.getStatus(),
            show.getCreatedAt(),
            show.getUpdatedAt()
        );
    }

    /**
     * Convert DTO to new entity (for creation).
     * Event must be fetched separately and set.
     */
    public Show toEntity(ShowDto dto, Event event) {
        if (dto == null) {
            return null;
        }
        Show show = new Show(
            event,
            dto.getStartTime(),
            dto.getEndTime(),
            dto.getVenue()
        );
        show.setStatus(dto.getStatus());
        return show;
    }

    /**
     * Update existing entity from DTO (for updates).
     */
    public void updateEntity(ShowDto dto, Show show) {
        if (dto == null || show == null) {
            return;
        }
        show.setStartTime(dto.getStartTime());
        show.setEndTime(dto.getEndTime());
        show.setVenue(dto.getVenue());
        show.setStatus(dto.getStatus());
    }
}
