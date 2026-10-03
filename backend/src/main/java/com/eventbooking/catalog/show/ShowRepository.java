package com.eventbooking.catalog.show;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for Show entity.
 */
@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    /**
     * Find all shows for a given event, ordered by start time.
     */
    @Query("SELECT s FROM Show s WHERE s.event.id = :eventId ORDER BY s.startTime ASC")
    List<Show> findByEventIdOrderByStartTimeAsc(@Param("eventId") Long eventId);

    /**
     * Find shows by event with pagination.
     */
    Page<Show> findByEventId(Long eventId, Pageable pageable);

    /**
     * Find upcoming shows (start time in the future) for an event.
     */
    @Query("SELECT s FROM Show s WHERE s.event.id = :eventId AND s.startTime > :now AND s.status = 'SCHEDULED' ORDER BY s.startTime ASC")
    List<Show> findUpcomingShowsByEventId(@Param("eventId") Long eventId, @Param("now") Instant now);

    /**
     * Check if a show has any confirmed bookings.
     * Used to determine if a show can be deleted or modified.
     * This will be implemented in Phase 4 when the Booking entity is created.
     */
    default boolean hasConfirmedBookings(Long showId) {
        // TODO: Implement in Phase 4 when Booking entity exists
        // For now, return false to allow show modifications
        return false;
    }
}
