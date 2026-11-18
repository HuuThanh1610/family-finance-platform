package com.familyfinance.profile.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserPreferenceResponse(
        UUID id,
        String preferenceKey,
        Map<String, Object> preferenceValue,
        Instant createdAt,
        Instant updatedAt
) {
}