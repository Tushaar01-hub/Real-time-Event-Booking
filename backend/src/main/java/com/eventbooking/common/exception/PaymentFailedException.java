package com.eventbooking.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentFailedException extends ApiException {
    public PaymentFailedException(String reason) {
        super(HttpStatus.PAYMENT_REQUIRED, "PAYMENT_FAILED", "Payment failed: " + reason);
    }
}
