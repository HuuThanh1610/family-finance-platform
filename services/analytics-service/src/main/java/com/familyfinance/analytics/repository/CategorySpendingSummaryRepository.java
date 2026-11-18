package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.CategorySpendingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategorySpendingSummaryRepository extends JpaRepository<CategorySpendingSummary, UUID> {

    List<CategorySpendingSummary> findByFamilyIdAndSummaryMonth(UUID familyId, LocalDate summaryMonth);

    Optional<CategorySpendingSummary> findByFamilyIdAndCategoryCodeAndSummaryMonth(
            UUID familyId, String categoryCode, LocalDate summaryMonth);
}