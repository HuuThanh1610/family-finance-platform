package com.familyfinance.payment.provider;

import com.familyfinance.payment.entity.PaymentAttempt;
import com.familyfinance.payment.entity.PaymentOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakePaymentAdapterTest {

    private FakePaymentAdapter adapter;
    private PaymentOrder order;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        adapter = new FakePaymentAdapter();
        order = new PaymentOrder();
        UUID orderId = UUID.randomUUID();
        order.setId(orderId);
        order.setAmount(500_000L);
        order.setPurpose("WALLET_TOPUP");

        attempt = new PaymentAttempt();
        attempt.setId(UUID.randomUUID());
        attempt.setPaymentOrder(order);
        attempt.setProviderCode(FakePaymentAdapter.PROVIDER_CODE);
        attempt.setInitiatedAt(Instant.now());
    }

    @Test
    void initiate_returnsPayUrlAndMetadata() {
        String customUrl = "https://fake.test/pay/custom";
        adapter.configure(order.getId(), customUrl, "PENDING");

        ProviderInitiateResult result = adapter.initiate(order, attempt);

        assertEquals(customUrl, result.payUrl());
        assertEquals("fake-txn-" + order.getId(), result.providerTxnId());
        assertEquals("PENDING", result.rawResponse().get("status"));
    }

    @Test
    void initiate_defaultBehavior_usesOrderIdInUrl() {
        ProviderInitiateResult result = adapter.initiate(order, attempt);

        assertTrue(result.payUrl().contains(order.getId().toString()));
        assertEquals(FakePaymentAdapter.PROVIDER_CODE, adapter.providerCode());
    }
}