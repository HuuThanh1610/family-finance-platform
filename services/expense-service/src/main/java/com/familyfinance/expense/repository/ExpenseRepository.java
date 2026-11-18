package com.familyfinance.expense.repository;

import com.familyfinance.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByFamilyIdOrderByExpenseDateDesc(UUID familyId);
}
