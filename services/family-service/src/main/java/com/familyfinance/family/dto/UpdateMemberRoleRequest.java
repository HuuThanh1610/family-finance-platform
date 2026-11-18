package com.familyfinance.family.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMemberRoleRequest(
        @NotBlank
        @Pattern(regexp = "OWNER|PARENT|ADULT_MEMBER|CHILD|VIEWER")
        String roleCode
) {
}