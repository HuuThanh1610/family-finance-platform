package com.familyfinance.profile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpsertPreferencesRequest(
        @NotEmpty List<@Valid PreferenceItemRequest> preferences
) {
}