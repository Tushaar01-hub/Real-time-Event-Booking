package com.eventbooking.catalog.event;

import com.eventbooking.catalog.category.Category;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Event entity and EventDto.
 * Keeps entities isolated from the API layer.
 */
@Component
public class EventMapper {

    /**
     * Convert entity to DTO.
     */
    public EventDto toDto(Event event) {
        if (event == null) {
            return null;
        }
        return new EventDto(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getCategory().getId(),
            event.getCategory().getName(),
            event.getLocation(),
            event.getPosterUrl(),
            event.getCreatedAt(),
            event.getUpdatedAt()
        );
    }

    /**
     * Convert DTO to new entity (for creation).
     * Category must be fetched separately and set.
     */
    public Event toEntity(EventDto dto, Category category) {
        if (dto == null) {
            return null;
        }
        return new Event(
            dto.getTitle(),
            dto.getDescription(),
            category,
            dto.getLocation(),
            dto.getPosterUrl()
        );
    }

    /**
     * Update existing entity from DTO (for updates).
     * Category must be fetched separately if changed.
     */
    public void updateEntity(EventDto dto, Event event, Category category) {
        if (dto == null || event == null) {
            return;
        }
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setCategory(category);
        event.setLocation(dto.getLocation());
        event.setPosterUrl(dto.getPosterUrl());
    }
}
