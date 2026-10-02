package com.eventbooking.common.exception;

import org.springframework.http.HttpStatus;

/** Base type for all expected, client-facing business errors. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** Stable machine-readable code, e.g. SEAT_ALREADY_HELD. */
    public String getCode() {
        return code;
    }
}
