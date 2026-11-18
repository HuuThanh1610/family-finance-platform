package com.familyfinance.wallet.repository;

import com.familyfinance.wallet.entity.WalletBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalletBalanceRepository extends JpaRepository<WalletBalance, UUID> {

    Optional<WalletBalance> findByLedgerAccountId(UUID ledgerAccountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from WalletBalance b where b.ledgerAccount.id = :ledgerAccountId")
    Optional<WalletBalance> findByLedgerAccountIdForUpdate(@Param("ledgerAccountId") UUID ledgerAccountId);
}
