package com.familyfinance.budget.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "allowance_requests")
public class AllowanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "requester_member_id", nullable = false)
    private UUID requesterMemberId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}