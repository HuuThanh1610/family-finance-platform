package com.familyfinance.budget.controller;

import com.familyfinance.budget.dto.BudgetResponse;
import com.familyfinance.budget.service.BudgetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public List<BudgetResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public BudgetResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
