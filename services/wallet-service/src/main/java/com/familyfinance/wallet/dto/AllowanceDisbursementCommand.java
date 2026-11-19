package com.familyfinance.wallet.dto;

import java.util.UUID;

/**
 * Internal command from Kafka AllowanceDisbursementRequested.
 * Wallets are resolved and validated by Wallet Service — not trusted from the client.
 */
public record AllowanceDisbursementCommand(
        UUID allowanceRequestId,
        UUID familyId,
        UUID requesterMemberId,
        long amount,
        String currencyCode
) {
}
