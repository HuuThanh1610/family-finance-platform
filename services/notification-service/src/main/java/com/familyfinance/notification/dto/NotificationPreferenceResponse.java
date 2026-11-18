package com.familyfinance.notification.dto;

import java.util.UUID;

public record NotificationPreferenceResponse(
        UUID id,
        UUID userProfileId,
        String channel,
        String category,
        boolean enabled
) {
}
