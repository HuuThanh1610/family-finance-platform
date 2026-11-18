package com.familyfinance.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Internal/API command used after verified payment provider success. */
public record TopUpCreditRequest(
        @NotNull UUID walletId,
        @NotNull @Min(1) Long amount,
        @NotBlank String referenceId,
        @NotBlank String idempotencyKey,
        String description
) {
}
