package com.familyfinance.budget.dto;

import java.time.Instant;
import java.util.UUID;

public record AllowanceRequestResponse(
        UUID id,
        UUID familyId,
        UUID requesterMemberId,
        Long amount,
        String reason,
        String status,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt
) {
}