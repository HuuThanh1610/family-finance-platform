package com.familyfinance.expense.service;

import com.familyfinance.expense.domain.ExpenseStatus;
import com.familyfinance.expense.dto.CreateExpenseRequest;
import com.familyfinance.expense.dto.ExpenseResponse;
import com.familyfinance.expense.dto.SubmitExpensePaymentRequest;
import com.familyfinance.expense.entity.Expense;
import com.familyfinance.expense.entity.ExpenseCategory;
import com.familyfinance.expense.entity.OutboxEvent;
import com.familyfinance.expense.exception.BusinessException;
import com.familyfinance.expense.exception.ResourceNotFoundException;
import com.familyfinance.expense.repository.ExpenseCategoryRepository;
import com.familyfinance.expense.repository.ExpenseRepository;
import com.familyfinance.expense.repository.OutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseService {

    public static final String EVENT_EXPENSE_PAYMENT_REQUESTED = "ExpensePaymentRequested";

    private final ExpenseRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final OutboxEventRepository outboxEventRepository;

    public ExpenseService(
            ExpenseRepository repository,
            ExpenseCategoryRepository categoryRepository,
            OutboxEventRepository outboxEventRepository
    ) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> findByFamily(UUID familyId) {
        return repository.findByFamilyIdOrderByExpenseDateDesc(familyId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(UUID id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    @Transactional
    public ExpenseResponse create(CreateExpenseRequest request) {
        Instant now = Instant.now();
        Expense expense = new Expense();
        expense.setFamilyId(request.familyId());
        expense.setPaidByMemberId(request.paidByMemberId());
        if (request.categoryId() != null) {
            ExpenseCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("ExpenseCategory", request.categoryId()));
            expense.setCategory(category);
        }
        expense.setTitle(request.title().trim());
        expense.setAmountTotal(request.amountTotal());
        expense.setExpenseDate(request.expenseDate());
        expense.setNotes(request.notes());
        expense.setStatus(ExpenseStatus.RECORDED.name());
        expense.setCreatedAt(now);
        expense.setUpdatedAt(now);
        return toResponse(repository.save(expense));
    }

    @Transactional
    public ExpenseResponse submitForPayment(UUID expenseId, SubmitExpensePaymentRequest request) {
        Expense expense = repository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        transition(expense, ExpenseStatus.SUBMITTED_FOR_PAYMENT);
        expense.setWalletId(request.walletId());
        expense.setPaymentReference("expense:" + expense.getId());
        expense.setUpdatedAt(Instant.now());
        transition(expense, ExpenseStatus.PENDING_WALLET);

        Map<String, Object> payload = new HashMap<>();
        payload.put("expenseId", expense.getId().toString());
        payload.put("familyId", expense.getFamilyId().toString());
        payload.put("walletId", request.walletId().toString());
        payload.put("amount", expense.getAmountTotal());
        payload.put("referenceId", expense.getPaymentReference());
        payload.put("idempotencyKey", "expense-pay:" + expense.getId());

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventType(EVENT_EXPENSE_PAYMENT_REQUESTED);
        outbox.setAggregateType("expense");
        outbox.setAggregateId(expense.getId());
        outbox.setPayload(payload);
        outbox.setCreatedAt(Instant.now());
        outbox.setStatus("PENDING");
        outboxEventRepository.save(outbox);

        return toResponse(repository.save(expense));
    }

    @Transactional
    public ExpenseResponse markPaid(UUID expenseId) {
        Expense expense = repository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        if (ExpenseStatus.valueOf(expense.getStatus()) == ExpenseStatus.PAID) {
            return toResponse(expense);
        }
        transition(expense, ExpenseStatus.PAID);
        expense.setUpdatedAt(Instant.now());
        return toResponse(repository.save(expense));
    }

    @Transactional
    public ExpenseResponse markFailed(UUID expenseId, String reason) {
        Expense expense = repository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        transition(expense, ExpenseStatus.FAILED);
        if (reason != null && !reason.isBlank()) {
            expense.setNotes((expense.getNotes() == null ? "" : expense.getNotes() + "\n") + "payment failed: " + reason);
        }
        expense.setUpdatedAt(Instant.now());
        return toResponse(repository.save(expense));
    }

    private static void transition(Expense expense, ExpenseStatus target) {
        ExpenseStatus current = ExpenseStatus.valueOf(expense.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new BusinessException(
                    "Cannot transition expense from " + current + " to " + target,
                    HttpStatus.CONFLICT);
        }
        expense.setStatus(target.name());
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
                entity.getWalletId(),
                entity.getPaymentReference(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
