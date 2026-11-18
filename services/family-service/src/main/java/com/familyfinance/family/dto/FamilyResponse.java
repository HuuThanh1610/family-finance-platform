package com.familyfinance.family.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record FamilyResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
