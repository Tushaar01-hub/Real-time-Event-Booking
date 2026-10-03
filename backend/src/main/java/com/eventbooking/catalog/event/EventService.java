package com.eventbooking.catalog.event;

import com.eventbooking.catalog.category.Category;
import com.eventbooking.catalog.category.CategoryRepository;
import com.eventbooking.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Event operations.
 * Handles CRUD, search, filtering, and pagination.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;

    public EventService(EventRepository eventRepository,
                        CategoryRepository categoryRepository,
                        EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.eventMapper = eventMapper;
    }

    /**
     * Search and filter events with pagination.
     * All filters are optional.
     */
    public Page<EventDto> searchEvents(String query, Long categoryId, String location, Pageable pageable) {

        Specification<Event> specification = Specification.where(null);

        if (query != null && !query.isBlank()) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            "%" + query.toLowerCase() + "%"
                    )
            );
        }

        if (categoryId != null) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(
                            root.get("category").get("id"),
                            categoryId
                    )
            );
        }

        if (location != null && !location.isBlank()) {
            specification = specification.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("location")),
                            "%" + location.toLowerCase() + "%"
                    )
            );
        }

        Page<Event> events = eventRepository.findAll(specification, pageable);

        return events.map(eventMapper::toDto);
    }

    /**
     * Get a single event by ID.
     */
    public EventDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        return eventMapper.toDto(event);
    }

    /**
     * Create a new event (admin only).
     */
    @Transactional
    public EventDto createEvent(EventDto dto) {
        // Validate category exists
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", dto.getCategoryId()));

        Event event = eventMapper.toEntity(dto, category);
        Event saved = eventRepository.save(event);
        return eventMapper.toDto(saved);
    }

    /**
     * Update an existing event (admin only).
     */
    @Transactional
    public EventDto updateEvent(Long id, EventDto dto) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        // Validate category exists if changed
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", dto.getCategoryId()));

        eventMapper.updateEntity(dto, event, category);
        Event updated = eventRepository.save(event);
        return eventMapper.toDto(updated);
    }

    /**
     * Delete an event (admin only).
     * This will fail if shows with confirmed bookings exist due to FK constraints.
     */
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event", id);
        }
        // If shows with confirmed bookings reference this event, the DB constraint will throw
        eventRepository.deleteById(id);
    }
}