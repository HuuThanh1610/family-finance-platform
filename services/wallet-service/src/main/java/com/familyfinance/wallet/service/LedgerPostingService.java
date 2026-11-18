package com.familyfinance.wallet.service;

import java.util.UUID;

/**
 * Double-entry ledger posting — intentionally not implemented in scaffold.
 */
public interface LedgerPostingService {

    /**
     * TODO: Post a balanced ledger transaction with idempotency — reserved learning topic.
     */
    UUID postTransaction(LedgerPostCommand command);

    record LedgerPostCommand(
            UUID walletAccountId,
            String referenceId,
            String idempotencyKey,
            String transactionType,
            String description
    ) {
    }
}
