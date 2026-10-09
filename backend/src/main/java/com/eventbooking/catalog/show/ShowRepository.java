package com.eventbooking.catalog.show;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
     * Find shows with most confirmed bookings.
     */
    @Query("SELECT s.event.id FROM Booking b JOIN b.show s WHERE b.status = 'CONFIRMED' GROUP BY s.event.id ORDER BY COUNT(b) DESC")
    Page<Long> findPopularEventIds(Pageable pageable);

    /**
     * Find upcoming events with scheduled shows.
     */
    @Query("""
    SELECT s.event.id
    FROM Show s
    WHERE s.startTime > :now
      AND s.status = 'SCHEDULED'
    GROUP BY s.event.id
    ORDER BY MIN(s.startTime) ASC
    """)
    Page<Long> findUpcomingEventIds(
            @Param("now") Instant now,
            Pageable pageable
    );

    /**
     * Find minimum price for an event across its scheduled shows.
     */
    @Query("SELECT MIN(se.price) FROM Show s JOIN s.event e JOIN Seat se ON se.show.id = s.id WHERE e.id = :eventId AND s.startTime > :now AND s.status = 'SCHEDULED'")
    BigDecimal findMinPriceForEvent(@Param("eventId") Long eventId, @Param("now") Instant now);

    /**
     * Find upcoming shows for a list of events.
     */
    @Query("SELECT s FROM Show s WHERE s.event.id IN :eventIds AND s.startTime > :now AND s.status = 'SCHEDULED'")
    List<Show> findUpcomingShowsByEventIds(@Param("eventIds") List<Long> eventIds, @Param("now") Instant now);

    /**
     * Check if a show has confirmed bookings.
     */
    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.show.id = :showId AND b.status = 'CONFIRMED'")
    boolean hasConfirmedBookings(@Param("showId") Long showId);
}
