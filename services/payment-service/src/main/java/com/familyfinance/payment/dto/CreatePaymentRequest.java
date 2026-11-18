package com.familyfinance.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID familyId,
        UUID payerMemberId,
        @NotNull UUID walletId,
        @NotNull @Positive Long amount,
        @NotBlank String purpose,
        @NotBlank String providerCode,
        String externalReference
) {
}