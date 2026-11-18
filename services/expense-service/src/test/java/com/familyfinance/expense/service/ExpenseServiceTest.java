package com.familyfinance.expense.service;

import com.familyfinance.expense.domain.ExpenseStatus;
import com.familyfinance.expense.dto.SubmitExpensePaymentRequest;
import com.familyfinance.expense.entity.Expense;
import com.familyfinance.expense.repository.ExpenseCategoryRepository;
import com.familyfinance.expense.repository.ExpenseRepository;
import com.familyfinance.expense.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock ExpenseRepository repository;
    @Mock ExpenseCategoryRepository categoryRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @InjectMocks ExpenseService service;

    @Test
    void submitForPaymentWritesOutboxAndPendingWallet() {
        UUID id = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        Expense expense = new Expense();
        expense.setId(id);
        expense.setFamilyId(UUID.randomUUID());
        expense.setTitle("Groceries");
        expense.setAmountTotal(50_000L);
        expense.setExpenseDate(LocalDate.now());
        expense.setStatus(ExpenseStatus.RECORDED.name());
        expense.setCreatedAt(Instant.now());
        expense.setUpdatedAt(Instant.now());

        when(repository.findById(id)).thenReturn(Optional.of(expense));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(outboxEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.submitForPayment(id, new SubmitExpensePaymentRequest(walletId));

        assertEquals(ExpenseStatus.PENDING_WALLET.name(), response.status());
        assertEquals(walletId, response.walletId());
        ArgumentCaptor<com.familyfinance.expense.entity.OutboxEvent> captor =
                ArgumentCaptor.forClass(com.familyfinance.expense.entity.OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertEquals(ExpenseService.EVENT_EXPENSE_PAYMENT_REQUESTED, captor.getValue().getEventType());
    }
}
