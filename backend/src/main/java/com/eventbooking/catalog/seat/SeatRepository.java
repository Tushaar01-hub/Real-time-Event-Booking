package com.eventbooking.catalog.seat;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for Seat entity.
 * Includes pessimistic locking for concurrency control.
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Find all seats for a show, ordered by section, row, and seat number.
     */
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId ORDER BY s.section, s.rowLabel, s.seatNumber")
    List<Seat> findByShowIdOrderBySectionAndPosition(@Param("showId") Long showId);

    /**
     * Find seats by IDs with pessimistic write lock (for booking confirmation).
     * Ordered by ID to prevent deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds ORDER BY s.id")
    List<Seat> findByIdInWithLock(@Param("seatIds") List<Long> seatIds);

    /**
     * Find available seats for a show (not booked, not blocked).
     */
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.status = 'AVAILABLE' ORDER BY s.section, s.rowLabel, s.seatNumber")
    List<Seat> findAvailableSeatsByShowId(@Param("showId") Long showId);

    /**
     * Check if all provided seat IDs belong to the same show and are available.
     */
    @Query("SELECT COUNT(s) = :expectedCount FROM Seat s WHERE s.id IN :seatIds AND s.show.id = :showId AND s.status = 'AVAILABLE'")
    boolean areAllSeatsAvailableForShow(@Param("seatIds") List<Long> seatIds,
                                        @Param("showId") Long showId,
                                        @Param("expectedCount") long expectedCount);

    /**
     * Delete all seats for a show (used when deleting a show).
     */
    @Modifying
    @Query("DELETE FROM Seat s WHERE s.show.id = :showId")
    void deleteByShowId(@Param("showId") Long showId);

    /**
     * Update seat status.
     */
    @Modifying
    @Query("UPDATE Seat s SET s.status = :status WHERE s.id = :seatId")
    void updateSeatStatus(@Param("seatId") Long seatId, @Param("status") String status);

    /**
     * Update seat price (admin only, not allowed if seat is booked).
     */
    @Modifying
    @Query("UPDATE Seat s SET s.price = :price WHERE s.id = :seatId AND s.status != 'BOOKED'")
    int updateSeatPrice(@Param("seatId") Long seatId, @Param("price") BigDecimal price);
}
