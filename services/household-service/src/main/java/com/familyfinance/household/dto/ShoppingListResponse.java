package com.familyfinance.household.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record ShoppingListResponse(
        UUID id,
        UUID familyId,
        String title,
        String status,
        LocalDate dueDate,
        UUID createdByMemberId,
        Instant createdAt,
        Instant updatedAt
) {
}
