package com.familyfinance.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotBlank String category,
        @NotNull Boolean email,
        @NotNull Boolean inApp,
        @NotNull Boolean push
) {
}