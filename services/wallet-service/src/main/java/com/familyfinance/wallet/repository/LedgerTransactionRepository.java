package com.familyfinance.wallet.repository;

import com.familyfinance.wallet.entity.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

    Optional<LedgerTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<LedgerTransaction> findByReferenceId(String referenceId);

    List<LedgerTransaction> findByWalletAccountIdOrderByPostedAtDesc(UUID walletAccountId);
}
