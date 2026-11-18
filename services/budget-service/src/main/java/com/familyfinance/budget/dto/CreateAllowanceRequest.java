package com.familyfinance.budget.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAllowanceRequest(
        @NotNull UUID familyId,
        @NotNull UUID requesterMemberId,
        @Positive long amount,
        @NotBlank @Size(max = 500) String reason
) {
}