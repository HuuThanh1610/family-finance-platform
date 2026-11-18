package com.familyfinance.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record AddBudgetCategoryRequest(
        @NotBlank String categoryCode,
        @NotBlank String displayName,
        @PositiveOrZero long allocatedAmount
) {
}