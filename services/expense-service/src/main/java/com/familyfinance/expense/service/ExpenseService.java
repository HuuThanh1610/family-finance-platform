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
import com.familyfinance.expense.security.FamilyAuthorizationClient;
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
    public static final String REF_PREFIX = "expense:";
    public static final String IDEMPOTENCY_PREFIX = "expense-pay:";
    public static final String PERMISSION_EXPENSE_CREATE = "expense.create";

    private final ExpenseRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final FamilyAuthorizationClient familyAuthorizationClient;

    public ExpenseService(
            ExpenseRepository repository,
            ExpenseCategoryRepository categoryRepository,
            OutboxEventRepository outboxEventRepository,
            FamilyAuthorizationClient familyAuthorizationClient
    ) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.familyAuthorizationClient = familyAuthorizationClient;
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
    public ExpenseResponse submitForPayment(
            UUID expenseId, SubmitExpensePaymentRequest request, UUID actorUserProfileId) {
        Expense expense = repository.findByIdForUpdate(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        familyAuthorizationClient.requirePermission(
                expense.getFamilyId(), actorUserProfileId, PERMISSION_EXPENSE_CREATE);

        ExpenseStatus current = ExpenseStatus.valueOf(expense.getStatus());
        if (current == ExpenseStatus.PENDING_WALLET || current == ExpenseStatus.PAID) {
            throw new BusinessException(
                    "Expense already " + current + "; cannot submit payment again",
                    HttpStatus.CONFLICT);
        }

        transition(expense, ExpenseStatus.SUBMITTED_FOR_PAYMENT);
        expense.setWalletId(request.walletId());
        expense.setPaymentReference(REF_PREFIX + expense.getId());
        expense.setUpdatedAt(Instant.now());
        transition(expense, ExpenseStatus.PENDING_WALLET);

        Map<String, Object> payload = new HashMap<>();
        payload.put("expenseId", expense.getId().toString());
        payload.put("familyId", expense.getFamilyId().toString());
        payload.put("walletId", request.walletId().toString());
        payload.put("amount", expense.getAmountTotal());
        payload.put("currencyCode", "VND");
        payload.put("referenceId", expense.getPaymentReference());
        payload.put("idempotencyKey", IDEMPOTENCY_PREFIX + expense.getId());

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

    /**
     * Apply WalletDebitPosted for EXPENSE_PAYMENT. Idempotent when already PAID.
     * Only Wallet ledger outcomes may complete payment — not a client shortcut.
     */
    @Transactional
    public ExpenseResponse applyWalletDebitPosted(
            UUID expenseId,
            UUID eventFamilyId,
            UUID eventWalletId,
            long eventAmount,
            String referenceId,
            String transactionType,
            UUID transactionId
    ) {
        if (!"EXPENSE_PAYMENT".equals(transactionType)) {
            throw new BusinessException("Not an expense payment debit", HttpStatus.BAD_REQUEST);
        }
        Expense expense = repository.findByIdForUpdate(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));

        if (ExpenseStatus.PAID.name().equals(expense.getStatus())) {
            return toResponse(expense);
        }

        String expectedRef = expense.getPaymentReference() != null
                ? expense.getPaymentReference()
                : REF_PREFIX + expense.getId();
        if (!expectedRef.equals(referenceId)) {
            throw new BusinessException("referenceId mismatch for expense payment", HttpStatus.CONFLICT);
        }
        if (!expense.getFamilyId().equals(eventFamilyId)) {
            throw new BusinessException("familyId mismatch for expense payment", HttpStatus.CONFLICT);
        }
        if (expense.getWalletId() == null || !expense.getWalletId().equals(eventWalletId)) {
            throw new BusinessException("walletId mismatch for expense payment", HttpStatus.CONFLICT);
        }
        if (expense.getAmountTotal() == null || expense.getAmountTotal() != eventAmount) {
            throw new BusinessException("amount mismatch for expense payment", HttpStatus.CONFLICT);
        }

        transition(expense, ExpenseStatus.PAID);
        if (transactionId != null) {
            String note = "ledgerTx=" + transactionId;
            expense.setNotes(expense.getNotes() == null || expense.getNotes().isBlank()
                    ? note
                    : expense.getNotes() + "\n" + note);
        }
        expense.setUpdatedAt(Instant.now());
        return toResponse(repository.save(expense));
    }

    @Transactional
    public ExpenseResponse applyPaymentFailed(UUID expenseId, String errorCode) {
        Expense expense = repository.findByIdForUpdate(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", expenseId));
        if (ExpenseStatus.FAILED.name().equals(expense.getStatus())) {
            return toResponse(expense);
        }
        if (ExpenseStatus.PAID.name().equals(expense.getStatus())) {
            return toResponse(expense);
        }
        transition(expense, ExpenseStatus.FAILED);
        String safe = errorCode == null || errorCode.isBlank() ? "PAYMENT_FAILED" : errorCode;
        expense.setNotes((expense.getNotes() == null ? "" : expense.getNotes() + "\n")
                + "payment failed: " + safe);
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
