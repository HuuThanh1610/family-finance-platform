package com.familyfinance.notification.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID recipientUserId,
        UUID familyId,
        String templateKey,
        String channel,
        String title,
        String body,
        String status,
        boolean read,
        Instant createdAt,
        Instant updatedAt
) {
}
