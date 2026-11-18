package com.familyfinance.wallet.dto;

import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID ledgerAccountId,
        String entryType,
        Long amount,
        String memo
) {
}
