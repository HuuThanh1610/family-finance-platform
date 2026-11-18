package com.familyfinance.expense.service;

import com.familyfinance.expense.dto.ExpenseResponse;
import com.familyfinance.expense.entity.Expense;
import com.familyfinance.expense.exception.ResourceNotFoundException;
import com.familyfinance.expense.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public List<ExpenseResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public ExpenseResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

        private ExpenseResponse toResponse(Expense entity) {
        return new ExpenseResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getPaidByMemberId(),
                entity.getCategory() != null ? entity.getCategory().getId() : null,
                entity.getTitle(),
                entity.getAmountTotal(),
                entity.getExpenseDate(),
                entity.getNotes(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
