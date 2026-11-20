package com.familyfinance.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "realized_financial_postings")
public class RealizedFinancialPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "ledger_transaction_id", nullable = false, unique = true)
    private UUID ledgerTransactionId;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "posting_kind", nullable = false, length = 32)
    private String postingKind;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
