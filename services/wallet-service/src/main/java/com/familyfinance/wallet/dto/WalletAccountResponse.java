package com.familyfinance.wallet.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.UUID;

public record WalletAccountResponse(
        UUID id,
        UUID familyId,
        UUID ownerMemberId,
        String name,
        String currencyCode,
        String walletType,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
