package com.familyfinance.analytics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record FamilyMonthlySummaryResponse(
        UUID id,
        UUID familyId,
        LocalDate summaryMonth,
        Long totalIncome,
        Long totalExpense,
        Long netCashflow,
        String currencyCode,
        Instant generatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
