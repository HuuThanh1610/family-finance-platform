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
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private LedgerTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ledger_account_id", nullable = false)
    private LedgerAccount ledgerAccount;

    @Column(name = "entry_type", nullable = false, length = 6)
    private String entryType;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 255)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
