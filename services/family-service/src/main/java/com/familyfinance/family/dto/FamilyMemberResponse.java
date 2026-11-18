package com.familyfinance.family.dto;

import java.time.Instant;
import java.util.UUID;

public record FamilyMemberResponse(
        UUID id,
        UUID familyId,
        UUID userProfileId,
        String roleCode,
        String status,
        String nickname,
        Instant joinedAt,
        Instant createdAt,
        Instant updatedAt
) {
}