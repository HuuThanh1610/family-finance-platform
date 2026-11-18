package com.familyfinance.profile.dto;

import java.time.Instant;
import java.util.UUID;

public record UserConsentResponse(
        UUID id,
        String consentType,
        String policyVersion,
        boolean granted,
        Instant grantedAt,
        Instant revokedAt,
        Instant createdAt,
        Instant updatedAt
) {
}