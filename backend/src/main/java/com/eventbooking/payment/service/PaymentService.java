package com.eventbooking.payment.service;

import com.eventbooking.payment.dto.PaymentResult;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private final Random random = new Random();
    private double successRate = 0.9;

    /**
     * Mock payment processing.
     * Succeeds ~90% of the time by default for realistic test coverage.
     * Override with {@link #setSuccessRate(double)} for deterministic tests.
     */
    public PaymentResult processPayment(UUID bookingId, String idempotencyKey) {
        // Simulate processing delay
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        boolean success = random.nextDouble() < successRate;
        if (success) {
            return new PaymentResult(true, null);
        } else {
            return new PaymentResult(false, "Payment declined by processor");
        }
    }

    /** Set the probability of a successful payment (0.0 to 1.0). */
    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
}
