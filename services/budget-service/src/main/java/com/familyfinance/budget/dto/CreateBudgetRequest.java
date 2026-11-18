package com.familyfinance.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.YearMonth;
import java.util.UUID;

public record CreateBudgetRequest(
        @NotNull UUID familyId,
        @NotBlank String name,
        @NotNull YearMonth month,
        @Positive long totalLimit,
        String currencyCode
) {
}