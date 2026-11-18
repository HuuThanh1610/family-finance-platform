package com.familyfinance.notification.dto;

import java.util.UUID;

public record NotificationTemplateResponse(
        UUID id,
        String templateKey,
        String channel,
        String locale,
        String subject,
        String bodyTemplate,
        boolean active
) {
}