package com.familyfinance.wallet.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LedgerTransactionResponse(
        UUID id,
        UUID familyId,
        String referenceId,
        String idempotencyKey,
        String description,
        String transactionType,
        Instant postedAt,
        List<LedgerEntryResponse> entries
) {
}
