package com.familyfinance.budget.repository;

import com.familyfinance.budget.entity.BudgetCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetCategoryRepository extends JpaRepository<BudgetCategory, UUID> {

    List<BudgetCategory> findByBudgetIdOrderByCategoryCodeAsc(UUID budgetId);

    Optional<BudgetCategory> findByBudgetIdAndCategoryCode(UUID budgetId, String categoryCode);
}