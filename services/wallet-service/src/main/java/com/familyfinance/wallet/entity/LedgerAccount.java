package com.familyfinance.wallet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ledger_accounts")
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_account_id", nullable = false)
    private WalletAccount walletAccount;

    @Column(name = "account_code", nullable = false, length = 64)
    private String accountCode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "account_kind", nullable = false, length = 32)
    private String accountKind;

    @Column(name = "normal_balance", nullable = false, length = 6)
    private String normalBalance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
