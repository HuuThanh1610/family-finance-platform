package com.familyfinance.payment.domain;

import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.exception.InvalidPaymentStateException;

import java.time.Instant;

public final class PaymentOrderStateMachine {

    private PaymentOrderStateMachine() {
    }

    public static void transition(PaymentOrder order, PaymentStatus target) {
        PaymentStatus current = PaymentStatus.fromDbValue(order.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new InvalidPaymentStateException(
                    "Cannot transition payment order %s from %s to %s".formatted(order.getId(), current, target));
        }
        order.setStatus(target.name());
        order.setUpdatedAt(Instant.now());
    }
}