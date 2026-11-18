package com.familyfinance.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotNull UUID familyId,
        UUID paidByMemberId,
        UUID categoryId,
        @NotBlank String title,
        @NotNull @Positive Long amountTotal,
        @NotNull LocalDate expenseDate,
        String notes
) {
}
