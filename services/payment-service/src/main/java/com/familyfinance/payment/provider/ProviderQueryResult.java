package com.familyfinance.payment.provider;

import java.util.Map;

public record ProviderQueryResult(
        boolean paid,
        long amount,
        String providerTxnId,
        int resultCode,
        Map<String, Object> rawResponse
) {
}