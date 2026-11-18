package com.familyfinance.budget.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "approval_history")
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allowance_request_id", nullable = false)
    private AllowanceRequest allowanceRequest;

    @Column(name = "approver_member_id", nullable = false)
    private UUID approverMemberId;

    @Column(nullable = false, length = 32)
    private String decision;

    @Column(length = 500)
    private String comment;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
}