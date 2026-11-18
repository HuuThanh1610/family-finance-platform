package com.familyfinance.family.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID familyId,
        UUID inviterMemberId,
        String inviteeEmail,
        String roleCode,
        String inviteToken,
        String status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt
) {
}