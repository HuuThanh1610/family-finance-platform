package com.familyfinance.payment.dto;

import java.util.UUID;

public record CreatePaymentResponse(
        UUID paymentOrderId,
        String status,
        String payUrl,
        String providerCode,
        PaymentOrderResponse order
) {
}