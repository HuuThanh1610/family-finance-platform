package com.familyfinance.wallet.dto;

import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID familyId,
        UUID ownerMemberId,
        String name,
        String currencyCode,
        String walletType,
        String status,
        Long availableBalance,
        Instant createdAt,
        Instant updatedAt
) {
}
