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
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_account_id", nullable = false)
    private WalletAccount walletAccount;

    @Column(name = "reference_id", nullable = false, length = 128)
    private String referenceId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(length = 500)
    private String description;

    @Column(name = "transaction_type", nullable = false, length = 64)
    private String transactionType;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
