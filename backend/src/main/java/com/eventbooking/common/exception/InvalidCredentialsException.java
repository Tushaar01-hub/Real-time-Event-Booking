package com.eventbooking.common.exception;

import org.springframework.http.HttpStatus;

/** Deliberately identical for "unknown email" and "wrong password" to avoid user enumeration. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
    }
}
