package com.familyfinance.payment.domain;

public enum PaymentStatus {
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    REFUNDED
}
