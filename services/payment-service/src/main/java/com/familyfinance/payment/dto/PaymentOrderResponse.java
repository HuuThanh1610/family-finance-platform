package com.familyfinance.payment.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentOrderResponse(
        UUID id,
        UUID familyId,
        UUID payerMemberId,
        Long amount,
        String currencyCode,
        String purpose,
        String externalReference,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {
}
