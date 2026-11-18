package com.familyfinance.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "provider_transactions")
public class ProviderTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_attempt_id", nullable = false)
    private PaymentAttempt paymentAttempt;

    @Column(name = "provider_txn_id", nullable = false, length = 200)
    private String providerTxnId;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 32)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawResponse = Map.of();

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}