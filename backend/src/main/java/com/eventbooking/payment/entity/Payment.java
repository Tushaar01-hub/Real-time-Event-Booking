package com.eventbooking.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status; // INITIATED | SUCCESS | FAILED | REFUNDED

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Payment() {}

    public Payment(UUID bookingId, BigDecimal amount, String idempotencyKey) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.status = "INITIATED";
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    public void succeed() { this.status = "SUCCESS"; }
    public void fail(String reason) { this.status = "FAILED"; this.failureReason = reason; }

    public Long getId() { return id; }
    public UUID getBookingId() { return bookingId; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
