package com.familyfinance.expense.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID familyId,
        UUID paidByMemberId,
        UUID categoryId,
        String title,
        Long amountTotal,
        LocalDate expenseDate,
        String notes,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
