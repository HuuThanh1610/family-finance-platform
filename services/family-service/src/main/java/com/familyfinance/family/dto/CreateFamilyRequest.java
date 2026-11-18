package com.familyfinance.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFamilyRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 120) String slug,
        @Size(max = 5000) String description
) {
}