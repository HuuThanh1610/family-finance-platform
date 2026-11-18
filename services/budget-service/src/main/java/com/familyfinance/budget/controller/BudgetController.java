package com.familyfinance.budget.controller;

import com.familyfinance.budget.dto.*;
import com.familyfinance.budget.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public List<BudgetResponse> list(@RequestParam(required = false) UUID familyId) {
        return service.findAll(familyId);
    }

    @GetMapping("/current")
    public BudgetResponse current(@RequestParam UUID familyId) {
        return service.findCurrent(familyId);
    }

    @GetMapping("/{id}")
    public BudgetResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody CreateBudgetRequest request) {
        return service.createMonthlyBudget(request);
    }

    @PostMapping("/{budgetId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetCategoryResponse addCategory(
            @PathVariable UUID budgetId,
            @Valid @RequestBody AddBudgetCategoryRequest request) {
        return service.addCategory(budgetId, request);
    }

    @PatchMapping("/categories/{categoryId}/limit")
    public BudgetCategoryResponse updateCategoryLimit(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryLimitRequest request) {
        return service.updateCategoryLimit(categoryId, request);
    }
}