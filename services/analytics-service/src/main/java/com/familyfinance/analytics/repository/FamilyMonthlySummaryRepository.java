package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.FamilyMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyMonthlySummaryRepository extends JpaRepository<FamilyMonthlySummary, UUID> {

    List<FamilyMonthlySummary> findByFamilyIdOrderBySummaryMonthAsc(UUID familyId);

    Optional<FamilyMonthlySummary> findByFamilyIdAndSummaryMonth(UUID familyId, LocalDate summaryMonth);
}
