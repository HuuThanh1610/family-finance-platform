package com.familyfinance.expense.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubmitExpensePaymentRequest(
        @NotNull UUID walletId
) {
}
