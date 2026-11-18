package com.familyfinance.payment.domain;

import com.familyfinance.payment.entity.PaymentOrder;
import com.familyfinance.payment.exception.InvalidPaymentStateException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentOrderStateMachineTest {

    @Test
    void allowsHappyPathTransitions() {
        PaymentOrder order = orderWithStatus(PaymentStatus.CREATED);
        assertDoesNotThrow(() -> PaymentOrderStateMachine.transition(order, PaymentStatus.PENDING_PROVIDER));

        PaymentOrderStateMachine.transition(order, PaymentStatus.PROVIDER_PAID);
        PaymentOrderStateMachine.transition(order, PaymentStatus.CREDIT_PENDING);
        PaymentOrderStateMachine.transition(order, PaymentStatus.COMPLETED);

        assertEqualsStatus(PaymentStatus.COMPLETED, order);
    }

    @Test
    void rejectsSkipTransition() {
        PaymentOrder order = orderWithStatus(PaymentStatus.CREATED);
        assertThrows(InvalidPaymentStateException.class,
                () -> PaymentOrderStateMachine.transition(order, PaymentStatus.COMPLETED));
    }

    private static PaymentOrder orderWithStatus(PaymentStatus status) {
        PaymentOrder order = new PaymentOrder();
        order.setId(UUID.randomUUID());
        order.setStatus(status.name());
        return order;
    }

    private static void assertEqualsStatus(PaymentStatus expected, PaymentOrder order) {
        if (!expected.name().equals(order.getStatus())) {
            throw new AssertionError("Expected " + expected + " but was " + order.getStatus());
        }
    }
}