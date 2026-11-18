package com.familyfinance.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpsertPreferenceRequest(
        @NotNull UUID userProfileId,
        @NotBlank String channel,
        @NotBlank String category,
        @NotNull Boolean enabled
) {
}
