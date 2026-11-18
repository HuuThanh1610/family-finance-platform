package com.familyfinance.budget.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RejectAllowanceRequest(
        @NotNull UUID approverMemberId,
        @Size(max = 500) String comment
) {
}