package com.familyfinance.budget.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID familyId,
        String name,
        LocalDate periodStart,
        LocalDate periodEnd,
        Long totalLimit,
        String currencyCode,
        String status,
        Long totalSpent,
        Long remainingAmount,
        boolean overBudget,
        List<BudgetCategoryResponse> categories,
        Instant createdAt,
        Instant updatedAt
) {
}
