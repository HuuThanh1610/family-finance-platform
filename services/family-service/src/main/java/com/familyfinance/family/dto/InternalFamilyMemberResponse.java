package com.familyfinance.family.dto;

import java.util.UUID;

/** Service-to-service member snapshot (userProfileId = notification recipient identity). */
public record InternalFamilyMemberResponse(
        UUID userProfileId,
        String roleCode,
        String status
) {
}
