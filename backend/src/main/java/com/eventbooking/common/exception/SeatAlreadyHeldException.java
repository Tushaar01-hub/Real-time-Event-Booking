package com.eventbooking.common.exception;

import org.springframework.http.HttpStatus;

public class SeatAlreadyHeldException extends ApiException {

    public SeatAlreadyHeldException(String seatLabels) {
        super(HttpStatus.CONFLICT, "SEAT_ALREADY_HELD",
                "Seat(s) " + seatLabels + " currently held by another user");
    }
}
