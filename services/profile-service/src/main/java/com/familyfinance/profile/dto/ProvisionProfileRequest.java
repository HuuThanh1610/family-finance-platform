package com.familyfinance.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvisionProfileRequest(
        @NotBlank @Size(max = 128) String keycloakSub,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(max = 200) String displayName
) {
}