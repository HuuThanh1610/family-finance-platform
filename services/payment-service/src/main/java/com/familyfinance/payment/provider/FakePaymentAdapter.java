package com.familyfinance.payment.provider;

import com.familyfinance.payment.entity.PaymentAttempt;
import com.familyfinance.payment.entity.PaymentOrder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakePaymentAdapter implements PaymentProvider {

    public static final String PROVIDER_CODE = "FAKE_PAYMENT";

    private final ConcurrentHashMap<UUID, FakePaymentBehavior> behaviors = new ConcurrentHashMap<>();

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public ProviderInitiateResult initiate(PaymentOrder order, PaymentAttempt attempt) {
        FakePaymentBehavior behavior = behaviors.getOrDefault(order.getId(), FakePaymentBehavior.defaults());
        String payUrl = behavior.payUrl() != null
                ? behavior.payUrl()
                : "https://fake-payment.local/pay/" + order.getId();
        return new ProviderInitiateResult(
                payUrl,
                "fake-req-" + attempt.getId(),
                "fake-txn-" + order.getId(),
                Map.of("status", behavior.status(), "payUrl", payUrl));
    }

    public void configure(UUID orderId, String payUrl, String status) {
        behaviors.put(orderId, new FakePaymentBehavior(payUrl, status));
    }

    public void reset() {
        behaviors.clear();
    }

    public record FakePaymentBehavior(String payUrl, String status) {
        static FakePaymentBehavior defaults() {
            return new FakePaymentBehavior(null, "PENDING");
        }
    }
}