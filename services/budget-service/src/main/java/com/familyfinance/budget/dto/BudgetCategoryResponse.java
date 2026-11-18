package com.familyfinance.budget.dto;

import java.util.UUID;

public record BudgetCategoryResponse(
        UUID id,
        String categoryCode,
        String displayName,
        Long allocatedAmount,
        Long spentAmount,
        Long remainingAmount,
        boolean overBudget
) {
}