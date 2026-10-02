package com.eventbooking.common.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Uniform error body returned by every failing endpoint. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    public record FieldViolation(String field, String message) {
    }

    public static ErrorResponse of(HttpStatus status, String code, String message, String path,
                                   List<FieldViolation> fieldErrors) {
        return new ErrorResponse(Instant.now(), status.value(), code, message, path, fieldErrors);
    }
}
