package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.WalletBalanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletBalanceSummaryRepository extends JpaRepository<WalletBalanceSummary, UUID> {

    List<WalletBalanceSummary> findByFamilyId(UUID familyId);

    Optional<WalletBalanceSummary> findByFamilyIdAndWalletId(UUID familyId, UUID walletId);
}