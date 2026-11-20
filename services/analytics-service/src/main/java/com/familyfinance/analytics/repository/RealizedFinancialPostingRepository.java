package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.RealizedFinancialPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RealizedFinancialPostingRepository extends JpaRepository<RealizedFinancialPosting, UUID> {

    boolean existsByLedgerTransactionId(UUID ledgerTransactionId);
}
