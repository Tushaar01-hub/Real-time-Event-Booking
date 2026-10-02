package com.eventbooking.common.exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but not allowed to do this (e.g. cancelling someone else's booking). */
public class UnauthorizedActionException extends ApiException {

    public UnauthorizedActionException(String message) {
        super(HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACTION", message);
    }
}
