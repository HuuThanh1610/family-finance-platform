package com.familyfinance.expense.controller;

import com.familyfinance.expense.dto.CreateExpenseRequest;
import com.familyfinance.expense.dto.ExpenseResponse;
import com.familyfinance.expense.dto.SubmitExpensePaymentRequest;
import com.familyfinance.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseV1Controller {

    private final ExpenseService service;

    public ExpenseV1Controller(ExpenseService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExpenseResponse> list(@RequestParam(required = false) UUID familyId) {
        if (familyId != null) {
            return service.findByFamily(familyId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody CreateExpenseRequest request) {
        return service.create(request);
    }

    @PostMapping("/{id}/submit-payment")
    public ExpenseResponse submitPayment(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitExpensePaymentRequest request
    ) {
        return service.submitForPayment(id, request);
    }

    @PostMapping("/{id}/mark-paid")
    public ExpenseResponse markPaid(@PathVariable UUID id) {
        return service.markPaid(id);
    }
}
