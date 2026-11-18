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
@Table(name = "webhook_inbox")
public class WebhookInbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "provider_code", nullable = false, length = 64)
    private String providerCode;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";
}