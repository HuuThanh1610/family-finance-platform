package com.familyfinance.wallet.repository;

import com.familyfinance.wallet.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByWalletAccountIdAndAccountCode(UUID walletAccountId, String accountCode);

    Optional<LedgerAccount> findByFamilyIdAndAccountCodeAndWalletAccountIsNull(UUID familyId, String accountCode);
}
