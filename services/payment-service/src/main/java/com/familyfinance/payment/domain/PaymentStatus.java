package com.familyfinance.payment.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PaymentStatus {
    CREATED,
    PENDING_PROVIDER,
    PROVIDER_PAID,
    CREDIT_PENDING,
    COMPLETED,
    FAILED,
    EXPIRED,
    CANCELLED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> TRANSITIONS = Map.of(
            CREATED, EnumSet.of(PENDING_PROVIDER, FAILED, CANCELLED),
            PENDING_PROVIDER, EnumSet.of(PROVIDER_PAID, FAILED, EXPIRED, CANCELLED),
            PROVIDER_PAID, EnumSet.of(CREDIT_PENDING, FAILED),
            CREDIT_PENDING, EnumSet.of(COMPLETED, FAILED),
            COMPLETED, EnumSet.noneOf(PaymentStatus.class),
            FAILED, EnumSet.noneOf(PaymentStatus.class),
            EXPIRED, EnumSet.noneOf(PaymentStatus.class),
            CANCELLED, EnumSet.noneOf(PaymentStatus.class)
    );

    public boolean canTransitionTo(PaymentStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static PaymentStatus fromDbValue(String value) {
        return PaymentStatus.valueOf(value);
    }
}