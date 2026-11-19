package com.familyfinance.wallet.dto;

import java.util.UUID;

public record ExpensePaymentCommand(
        UUID expenseId,
        UUID familyId,
        UUID walletId,
        long amount,
        String currencyCode,
        String referenceId,
        String idempotencyKey
) {
}
