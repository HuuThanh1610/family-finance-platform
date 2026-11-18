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
@Table(name = "wallet_accounts")
public class WalletAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "owner_member_id")
    private UUID ownerMemberId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(name = "wallet_type", nullable = false, length = 32)
    private String walletType = "FAMILY_POOL";

    @Column(nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
