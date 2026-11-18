package com.familyfinance.profile.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String keycloakSub,
        String email,
        String displayName,
        String phone,
        String avatarUrl,
        String locale,
        String timezone,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
