package com.familyfinance.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RecordConsentRequest(
        @NotBlank @Size(max = 64) String consentType,
        @NotBlank @Size(max = 32) String policyVersion,
        @NotNull Boolean granted
) {
}