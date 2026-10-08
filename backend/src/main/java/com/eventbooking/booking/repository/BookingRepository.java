package com.eventbooking.booking.repository;

import com.eventbooking.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Find all bookings for a user with pagination.
     */
    Page<Booking> findByUserId(Long userId, Pageable pageable);

    /**
     * Find all bookings with optional filtering for admins.
     */
    @Query("SELECT b FROM Booking b WHERE (:status IS NULL OR b.status = :status) AND (:showId IS NULL OR b.show.id = :showId)")
    Page<Booking> findAllAdmin(@Param("status") String status, @Param("showId") Long showId, Pageable pageable);

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
