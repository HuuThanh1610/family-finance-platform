package com.familyfinance.payment.provider;

import java.util.Map;

public record ProviderInitiateResult(
        String payUrl,
        String providerRequestId,
        String providerTxnId,
        Map<String, Object> rawResponse
) {
}