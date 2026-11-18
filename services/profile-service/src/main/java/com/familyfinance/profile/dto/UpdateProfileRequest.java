package com.familyfinance.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 200) String displayName,
        @Size(max = 32) String phone,
        @Size(max = 2048) String avatarUrl,
        @NotBlank @Size(max = 16) String locale,
        @NotBlank @Size(max = 64) String timezone
) {
}