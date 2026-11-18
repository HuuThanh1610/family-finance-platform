package com.familyfinance.analytics.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "budget_category_actual")
public class BudgetCategoryActual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "summary_month", nullable = false)
    private LocalDate summaryMonth;

    @Column(name = "category_code", nullable = false, length = 64)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 128)
    private String categoryName;

    @Column(name = "allocated_amount", nullable = false)
    private Long allocatedAmount = 0L;

    @Column(name = "actual_amount", nullable = false)
    private Long actualAmount = 0L;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "VND";

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}