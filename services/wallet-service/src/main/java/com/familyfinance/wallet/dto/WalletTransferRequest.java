package com.familyfinance.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WalletTransferRequest(
        @NotNull UUID sourceWalletId,
        @NotNull UUID destinationWalletId,
        @NotNull @Min(1) Long amount,
        @NotBlank String referenceId,
        @NotBlank String idempotencyKey,
        String description
) {
}
