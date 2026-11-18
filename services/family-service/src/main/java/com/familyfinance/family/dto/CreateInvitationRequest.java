package com.familyfinance.family.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateInvitationRequest(
        @NotBlank @Email @jakarta.validation.constraints.Size(max = 320) String inviteeEmail,
        @NotBlank
        @Pattern(regexp = "OWNER|PARENT|ADULT_MEMBER|CHILD|VIEWER")
        String roleCode
) {
}