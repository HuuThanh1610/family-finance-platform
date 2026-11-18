package com.familyfinance.budget.service;

import com.familyfinance.budget.dto.*;
import com.familyfinance.budget.entity.Budget;
import com.familyfinance.budget.entity.BudgetCategory;
import com.familyfinance.budget.exception.BusinessException;
import com.familyfinance.budget.exception.ResourceNotFoundException;
import com.familyfinance.budget.repository.BudgetCategoryRepository;
import com.familyfinance.budget.repository.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final BudgetCategoryRepository categoryRepository;

    public BudgetService(BudgetRepository budgetRepository, BudgetCategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> findAll(UUID familyId) {
        List<Budget> budgets = familyId == null
                ? budgetRepository.findAll()
                : budgetRepository.findByFamilyIdOrderByPeriodStartDesc(familyId);
        return budgets.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse findById(UUID id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", id));
        return toResponse(budget);
    }

    @Transactional(readOnly = true)
    public BudgetResponse findCurrent(UUID familyId) {
        if (familyId == null) {
            throw new BusinessException("familyId is required", HttpStatus.BAD_REQUEST);
        }
        LocalDate today = LocalDate.now();
        Budget budget = budgetRepository
                .findFirstByFamilyIdAndStatusAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
                        familyId, "ACTIVE", today, today)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active budget for family " + familyId + " on " + today));
        return toResponse(budget);
    }

    @Transactional
    public BudgetResponse createMonthlyBudget(CreateBudgetRequest request) {
        Instant now = Instant.now();
        LocalDate periodStart = request.month().atDay(1);
        LocalDate periodEnd = request.month().atEndOfMonth();

        Budget budget = new Budget();
        budget.setFamilyId(request.familyId());
        budget.setName(request.name());
        budget.setPeriodStart(periodStart);
        budget.setPeriodEnd(periodEnd);
        budget.setTotalLimit(request.totalLimit());
        budget.setCurrencyCode(request.currencyCode() != null ? request.currencyCode() : "VND");
        budget.setStatus("ACTIVE");
        budget.setCreatedAt(now);
        budget.setUpdatedAt(now);
        budget = budgetRepository.save(budget);
        return toResponse(budget);
    }

    @Transactional
    public BudgetCategoryResponse addCategory(UUID budgetId, AddBudgetCategoryRequest request) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", budgetId));
        categoryRepository.findByBudgetIdAndCategoryCode(budgetId, request.categoryCode()).ifPresent(existing -> {
            throw new BusinessException("Category code already exists: " + request.categoryCode(), HttpStatus.CONFLICT);
        });

        Instant now = Instant.now();
        BudgetCategory category = new BudgetCategory();
        category.setBudget(budget);
        category.setCategoryCode(request.categoryCode());
        category.setDisplayName(request.displayName());
        category.setAllocatedAmount(request.allocatedAmount());
        category.setSpentAmount(0L);
        category.setCreatedAt(now);
        category.setUpdatedAt(now);
        category = categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    @Transactional
    public BudgetCategoryResponse updateCategoryLimit(UUID categoryId, UpdateCategoryLimitRequest request) {
        BudgetCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetCategory", categoryId));
        category.setAllocatedAmount(request.allocatedAmount());
        category.setUpdatedAt(Instant.now());
        category = categoryRepository.save(category);
        return toCategoryResponse(category);
    }

    private BudgetResponse toResponse(Budget entity) {
        List<BudgetCategory> categories = categoryRepository.findByBudgetIdOrderByCategoryCodeAsc(entity.getId());
        long totalSpent = categories.stream().mapToLong(BudgetCategory::getSpentAmount).sum();
        long remaining = entity.getTotalLimit() - totalSpent;
        boolean overBudget = totalSpent > entity.getTotalLimit();
        return new BudgetResponse(
                entity.getId(),
                entity.getFamilyId(),
                entity.getName(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getTotalLimit(),
                entity.getCurrencyCode(),
                entity.getStatus(),
                totalSpent,
                remaining,
                overBudget,
                categories.stream().map(this::toCategoryResponse).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BudgetCategoryResponse toCategoryResponse(BudgetCategory category) {
        long remaining = category.getAllocatedAmount() - category.getSpentAmount();
        boolean overBudget = category.getSpentAmount() > category.getAllocatedAmount();
        return new BudgetCategoryResponse(
                category.getId(),
                category.getCategoryCode(),
                category.getDisplayName(),
                category.getAllocatedAmount(),
                category.getSpentAmount(),
                remaining,
                overBudget
        );
    }
}