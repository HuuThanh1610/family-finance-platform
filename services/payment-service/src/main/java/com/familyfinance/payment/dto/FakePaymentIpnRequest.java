package com.familyfinance.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FakePaymentIpnRequest(
        @NotNull UUID orderId,
        Long amount,
        String providerTxnId
) {
}