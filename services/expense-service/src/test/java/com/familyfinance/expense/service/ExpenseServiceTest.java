package com.familyfinance.expense.service;

import com.familyfinance.expense.domain.ExpenseStatus;
import com.familyfinance.expense.dto.SubmitExpensePaymentRequest;
import com.familyfinance.expense.entity.Expense;
import com.familyfinance.expense.exception.BusinessException;
import com.familyfinance.expense.repository.ExpenseCategoryRepository;
import com.familyfinance.expense.repository.ExpenseRepository;
import com.familyfinance.expense.repository.OutboxEventRepository;
import com.familyfinance.expense.security.FamilyAuthorizationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository repository;
    @Mock ExpenseCategoryRepository categoryRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock FamilyAuthorizationClient familyAuthorizationClient;

    ExpenseService service;
    UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExpenseService(repository, categoryRepository, outboxEventRepository, familyAuthorizationClient);
    }

    @Test
    void submitForPaymentWritesOutboxAndPendingWallet() {
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Expense expense = recorded(id, familyId);

        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(expense));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.submitForPayment(id, new SubmitExpensePaymentRequest(walletId), actor);

        assertThat(response.status()).isEqualTo(ExpenseStatus.PENDING_WALLET.name());
        assertThat(response.walletId()).isEqualTo(walletId);
        verify(familyAuthorizationClient).requirePermission(eq(familyId), eq(actor), eq("expense.create"));
        ArgumentCaptor<com.familyfinance.expense.entity.OutboxEvent> captor =
                ArgumentCaptor.forClass(com.familyfinance.expense.entity.OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo(ExpenseService.EVENT_EXPENSE_PAYMENT_REQUESTED);
        assertThat(captor.getValue().getPayload())
                .containsEntry("currencyCode", "VND")
                .containsEntry("idempotencyKey", "expense-pay:" + id)
                .containsEntry("referenceId", "expense:" + id);
    }

    @Test
    void duplicateSubmitWhilePendingRejected() {
        UUID id = UUID.randomUUID();
        Expense expense = recorded(id, UUID.randomUUID());
        expense.setStatus(ExpenseStatus.PENDING_WALLET.name());
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> service.submitForPayment(id, new SubmitExpensePaymentRequest(UUID.randomUUID()), actor))
                .isInstanceOf(BusinessException.class);
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void applyWalletDebitPostedMarksPaid() {
        UUID id = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Expense expense = recorded(id, familyId);
        expense.setStatus(ExpenseStatus.PENDING_WALLET.name());
        expense.setWalletId(walletId);
        expense.setPaymentReference("expense:" + id);
        expense.setAmountTotal(50_000L);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(expense));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.applyWalletDebitPosted(
                id, familyId, walletId, 50_000L, "expense:" + id, "EXPENSE_PAYMENT", UUID.randomUUID());

        assertThat(response.status()).isEqualTo(ExpenseStatus.PAID.name());
    }

    @Test
    void mismatchedFamilyCannotMarkPaid() {
        UUID id = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Expense expense = recorded(id, familyId);
        expense.setStatus(ExpenseStatus.PENDING_WALLET.name());
        expense.setWalletId(walletId);
        expense.setPaymentReference("expense:" + id);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> service.applyWalletDebitPosted(
                id, UUID.randomUUID(), walletId, 50_000L, "expense:" + id, "EXPENSE_PAYMENT", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void applyPaymentFailedFromPending() {
        UUID id = UUID.randomUUID();
        Expense expense = recorded(id, UUID.randomUUID());
        expense.setStatus(ExpenseStatus.PENDING_WALLET.name());
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(expense));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.applyPaymentFailed(id, "INSUFFICIENT_FUNDS");

        assertThat(response.status()).isEqualTo(ExpenseStatus.FAILED.name());
        assertThat(response.notes()).contains("INSUFFICIENT_FUNDS");
    }

    private Expense recorded(UUID id, UUID familyId) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setFamilyId(familyId);
        expense.setTitle("Groceries");
        expense.setAmountTotal(50_000L);
        expense.setExpenseDate(LocalDate.now());
        expense.setStatus(ExpenseStatus.RECORDED.name());
        expense.setCreatedAt(Instant.now());
        expense.setUpdatedAt(Instant.now());
        return expense;
    }
}
