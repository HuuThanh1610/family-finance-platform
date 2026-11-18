package com.familyfinance.budget.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
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
        Instant createdAt,
        Instant updatedAt
) {
}
