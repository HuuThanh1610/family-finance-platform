package com.familyfinance.budget.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCategoryLimitRequest(
        @PositiveOrZero long allocatedAmount
) {
}