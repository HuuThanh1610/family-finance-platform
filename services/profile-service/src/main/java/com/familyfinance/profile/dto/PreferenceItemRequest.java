package com.familyfinance.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PreferenceItemRequest(
        @NotBlank @Size(max = 128) String preferenceKey,
        @NotNull Map<String, Object> preferenceValue
) {
}