package com.eventbooking.booking.repository;

import com.eventbooking.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Find all bookings for a user, ordered by creation date descending.
     */
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all PENDING bookings that have expired (for the sweeper).
     */
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    List<Booking> findExpiredPendingBookings(@Param("now") Instant now);

    /**
     * Find all bookings for a specific show.
     */
    List<Booking> findByShowId(Long showId);
}
