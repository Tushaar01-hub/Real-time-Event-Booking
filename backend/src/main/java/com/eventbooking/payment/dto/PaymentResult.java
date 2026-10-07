package com.eventbooking.payment.dto;

public record PaymentResult(boolean success, String failureReason) {}
