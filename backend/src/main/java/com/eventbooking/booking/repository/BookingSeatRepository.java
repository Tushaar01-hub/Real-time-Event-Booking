package com.eventbooking.booking.repository;

import com.eventbooking.booking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /**
     * Find all booking seats for a specific booking.
     */
    List<BookingSeat> findByBookingId(UUID bookingId);
}
