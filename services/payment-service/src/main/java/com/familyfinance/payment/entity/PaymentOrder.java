package com.familyfinance.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "payment_orders")
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "payer_member_id")
    private UUID payerMemberId;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(nullable = false, length = 64)
    private String purpose;

    @Column(name = "external_reference", length = 128)
    private String externalReference;

    @Column(nullable = false, length = 32)
    private String status = "CREATED";

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
