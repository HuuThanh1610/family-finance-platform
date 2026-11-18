package com.familyfinance.wallet.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "wallet_balances")
public class WalletBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_account_id", nullable = false, unique = true)
    private LedgerAccount ledgerAccount;

    @Column(name = "balance_amount", nullable = false)
    private Long balanceAmount = 0L;

    @Version
    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "recalculated_at", nullable = false)
    private Instant recalculatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
