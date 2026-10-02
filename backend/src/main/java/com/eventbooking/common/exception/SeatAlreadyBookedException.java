package com.eventbooking.common.exception;

import org.springframework.http.HttpStatus;

public class SeatAlreadyBookedException extends ApiException {

    public SeatAlreadyBookedException(String seatLabels) {
        super(HttpStatus.CONFLICT, "SEAT_ALREADY_BOOKED",
                "Seat(s) " + seatLabels + " already booked");
    }
}
