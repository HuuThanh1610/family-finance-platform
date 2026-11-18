package com.familyfinance.budget.service;

import com.familyfinance.budget.dto.BudgetResponse;
import com.familyfinance.budget.entity.Budget;
import com.familyfinance.budget.exception.ResourceNotFoundException;
import com.familyfinance.budget.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BudgetService {

    private final BudgetRepository repository;

    public BudgetService(BudgetRepository repository) {
        this.repository = repository;
    }

    public List<BudgetResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public BudgetResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
    }

        private BudgetResponse toResponse(Budget entity) {
        return new BudgetResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getName(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getTotalLimit(),
                entity.getCurrencyCode(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
