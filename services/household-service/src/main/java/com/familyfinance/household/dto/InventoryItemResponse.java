package com.familyfinance.household.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryItemResponse(
        UUID id,
        UUID familyId,
        UUID catalogItemId,
        String locationCode,
        BigDecimal quantityOnHand,
        BigDecimal reorderLevel,
        boolean lowStock,
        Instant updatedAt
) {
}
