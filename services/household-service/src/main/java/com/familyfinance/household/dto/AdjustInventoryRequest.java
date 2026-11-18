package com.familyfinance.household.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustInventoryRequest(
        @NotBlank String movementType,
        @NotNull BigDecimal quantityDelta,
        String reason,
        String referenceId
) {
}
