package com.familyfinance.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAllowancePolicyRequest(
        @NotNull UUID familyId,
        @NotNull UUID memberId,
        @Positive long amount,
        @NotBlank String cadence,
        Short dayOfWeek,
        Short dayOfMonth,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo
) {
}