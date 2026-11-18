package com.familyfinance.wallet.repository;

import com.familyfinance.wallet.entity.WalletAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletAccountRepository extends JpaRepository<WalletAccount, UUID> {
}
