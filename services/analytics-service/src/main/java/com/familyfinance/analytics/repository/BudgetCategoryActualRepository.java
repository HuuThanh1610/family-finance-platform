package com.familyfinance.analytics.repository;

import com.familyfinance.analytics.entity.BudgetCategoryActual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetCategoryActualRepository extends JpaRepository<BudgetCategoryActual, UUID> {

    List<BudgetCategoryActual> findByFamilyIdAndSummaryMonthOrderByCategoryCodeAsc(
            UUID familyId, LocalDate summaryMonth);

    Optional<BudgetCategoryActual> findByFamilyIdAndCategoryCodeAndSummaryMonth(
            UUID familyId, String categoryCode, LocalDate summaryMonth);
}