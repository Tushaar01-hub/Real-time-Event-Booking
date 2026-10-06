package com.eventbooking.booking.entity;

import com.eventbooking.catalog.seat.Seat;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * BookingSeat represents the many-to-many relationship between Booking and Seat.
 * The 'active' flag is true only for CONFIRMED bookings.
 * A partial unique index on (seat_id) WHERE active=true prevents double booking.
 */
@Entity
@Table(name = "booking_seats")
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @NotNull
    @DecimalMin("0.0")
    @Column(name = "price_at_booking", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtBooking;

    @Column(nullable = false)
    private Boolean active = false;

    // ---------------------------------------------------------------- constructors

    protected BookingSeat() {
        // JPA requires a no-arg constructor
    }

    public BookingSeat(Booking booking, Seat seat, BigDecimal priceAtBooking) {
        this.booking = booking;
        this.seat = seat;
        this.priceAtBooking = priceAtBooking;
        this.active = false;
    }

    // ---------------------------------------------------------------- business methods

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    // ---------------------------------------------------------------- getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public BigDecimal getPriceAtBooking() {
        return priceAtBooking;
    }

    public void setPriceAtBooking(BigDecimal priceAtBooking) {
        this.priceAtBooking = priceAtBooking;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
