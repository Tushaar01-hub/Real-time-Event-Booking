package com.eventbooking.common.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;

public class BookingExpiredException extends ApiException {

    public BookingExpiredException(UUID bookingId) {
        super(HttpStatus.GONE, "BOOKING_EXPIRED", "Booking " + bookingId + " has expired");
    }
}
