package com.familyfinance.budget.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AllowancePolicyResponse(
        UUID id,
        UUID familyId,
        UUID memberId,
        Long amount,
        String cadence,
        Short dayOfWeek,
        Short dayOfMonth,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}