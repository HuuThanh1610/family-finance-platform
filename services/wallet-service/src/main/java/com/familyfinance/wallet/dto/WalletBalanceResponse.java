package com.familyfinance.wallet.dto;

import java.util.UUID;

public record WalletBalanceResponse(
        UUID walletId,
        String currencyCode,
        Long availableBalance,
        Integer version
) {
}
