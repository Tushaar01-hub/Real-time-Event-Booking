package com.eventbooking.catalog.show;

import com.eventbooking.catalog.event.Event;
import com.eventbooking.catalog.event.EventRepository;
import com.eventbooking.catalog.seat.Seat;
import com.eventbooking.catalog.seat.SeatRepository;
import com.eventbooking.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for Show operations.
 * Handles CRUD and coordinates with seat generation.
 */
@Service
@Transactional(readOnly = true)
public class ShowService {

    private final ShowRepository showRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final ShowMapper showMapper;

    public ShowService(ShowRepository showRepository,
                      EventRepository eventRepository,
                      SeatRepository seatRepository,
                      ShowMapper showMapper) {
        this.showRepository = showRepository;
        this.eventRepository = eventRepository;
        this.seatRepository = seatRepository;
        this.showMapper = showMapper;
    }

    /**
     * Get all shows for a given event.
     */
    public List<ShowDto> getShowsByEventId(Long eventId) {
        List<Show> shows = showRepository.findByEventIdOrderByStartTimeAsc(eventId);
        return shows.stream()
                .map(showMapper::toDto)
                .toList();
    }

    /**
     * Get a single show by ID.
     */
    public ShowDto getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        return showMapper.toDto(show);
    }

    /**
     * Create a new show with seat layout (admin only).
     * Generates seats based on the provided layout specification.
     */
    @Transactional
    public ShowDto createShow(CreateShowRequest request) {
        // Validate event exists
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", request.getEventId()));

        // Validate time range
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Create the show
        Show show = new Show(event, request.getStartTime(), request.getEndTime(), request.getVenue());
        Show savedShow = showRepository.save(show);

        // Generate seats from layout
        List<Seat> seats = generateSeatsFromLayout(savedShow, request.getSeatLayout());
        seatRepository.saveAll(seats);

        return showMapper.toDto(savedShow);
    }

    /**
     * Update an existing show (admin only).
     * Layout cannot be changed once bookings exist.
     */
    @Transactional
    public ShowDto updateShow(Long id, ShowDto dto) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));

        // Check if layout modification is being attempted (not supported in update)
        // Layout is immutable once show is created with seats

        // Validate time range
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // Prevent certain changes if confirmed bookings exist
        if (showRepository.hasConfirmedBookings(id)) {
            // Only allow status changes to CANCELLED
            if (!show.getStatus().equals(dto.getStatus()) && !"CANCELLED".equals(dto.getStatus())) {
                throw new IllegalArgumentException("Cannot modify show with confirmed bookings except to cancel it");
            }
        }

        showMapper.updateEntity(dto, show);
        Show updated = showRepository.save(show);
        return showMapper.toDto(updated);
    }

    /**
     * Delete a show (admin only).
     * Will fail if confirmed bookings exist.
     */
    @Transactional
    public void deleteShow(Long id) {
        if (!showRepository.existsById(id)) {
            throw new ResourceNotFoundException("Show", id);
        }

        // Check for confirmed bookings
        if (showRepository.hasConfirmedBookings(id)) {
            throw new IllegalArgumentException("Cannot delete show with confirmed bookings. Cancel it instead.");
        }

        // Delete seats first (cascade should handle this, but being explicit)
        seatRepository.deleteByShowId(id);
        showRepository.deleteById(id);
    }

    /**
     * Generate seats from layout specification.
     * Creates seat rows with labels (A, B, C, ..., Z, AA, AB, ...)
     */
    private List<Seat> generateSeatsFromLayout(Show show, List<SeatLayoutDto> layout) {
        List<Seat> seats = new ArrayList<>();

        for (SeatLayoutDto section : layout) {
            for (int row = 0; row < section.getRows(); row++) {
                String rowLabel = generateRowLabel(row);

                for (int seatNum = 1; seatNum <= section.getSeatsPerRow(); seatNum++) {
                    Seat seat = new Seat(
                        show,
                        rowLabel,
                        seatNum,
                        section.getSection(),
                        section.getPrice()
                    );
                    seats.add(seat);
                }
            }
        }

        return seats;
    }

    /**
     * Generate row labels: A, B, C, ..., Z, AA, AB, ..., AZ, BA, ...
     */
    private String generateRowLabel(int rowIndex) {
        StringBuilder label = new StringBuilder();
        int index = rowIndex;

        do {
            label.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        } while (index >= 0);

        return label.toString();
    }
}
