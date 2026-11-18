package com.familyfinance.budget.repository;

import com.familyfinance.budget.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByFamilyIdOrderByPeriodStartDesc(UUID familyId);

    Optional<Budget> findFirstByFamilyIdAndStatusAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
            UUID familyId, String status, LocalDate periodStart, LocalDate periodEnd);
}
