package com.familyfinance.wallet.service;

import com.familyfinance.wallet.domain.EntryType;
import com.familyfinance.wallet.entity.LedgerTransaction;

import java.util.List;
import java.util.UUID;

/**
 * Double-entry ledger posting — sole writer of journal transactions and balance projections.
 */
public interface LedgerPostingService {

    LedgerTransaction postBalancedTransaction(PostCommand command);

    LedgerTransaction reverseTransaction(UUID transactionId, String reversalReferenceId, String idempotencyKey);

    record PostCommand(
            UUID familyId,
            UUID primaryWalletId,
            String referenceId,
            String idempotencyKey,
            String transactionType,
            String description,
            List<EntryLine> entries
    ) {
    }

    record EntryLine(
            UUID ledgerAccountId,
            EntryType entryType,
            long amount,
            String memo
    ) {
    }
}
