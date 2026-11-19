package com.familyfinance.wallet;

import com.familyfinance.wallet.domain.WalletType;
import com.familyfinance.wallet.dto.CreateWalletRequest;
import com.familyfinance.wallet.dto.ExpensePaymentCommand;
import com.familyfinance.wallet.dto.LedgerTransactionResponse;
import com.familyfinance.wallet.dto.WalletResponse;
import com.familyfinance.wallet.entity.LedgerAccount;
import com.familyfinance.wallet.entity.OutboxEvent;
import com.familyfinance.wallet.entity.WalletAccount;
import com.familyfinance.wallet.exception.PermanentExpensePaymentException;
import com.familyfinance.wallet.repository.OutboxEventRepository;
import com.familyfinance.wallet.service.WalletApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletExpensePaymentIT extends WalletIntegrationTestBase {

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void expensePaymentPostsBalancedJournalAndDebitOutboxOnce() {
        UUID familyId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        WalletResponse pool = createFamilyPool(familyId, "Expense-pool");
        seedCredit(pool.id(), 200_000L, "exp-seed");

        ExpensePaymentCommand cmd = new ExpensePaymentCommand(
                expenseId, familyId, pool.id(), 40_000L, "VND",
                "expense:" + expenseId, "expense-pay:" + expenseId);

        LedgerTransactionResponse first = walletApplicationService.payExpense(cmd);
        LedgerTransactionResponse second = walletApplicationService.payExpense(cmd);

        assertEquals(first.id(), second.id());
        assertEquals(160_000L, walletApplicationService.getBalance(pool.id(), ACTOR).availableBalance());
        assertWalletProjectionMatchesJournal(pool.id());

        LedgerAccount clearing = ledgerAccountRepository
                .findByFamilyIdAndAccountCodeAndWalletAccountIsNull(
                        familyId, WalletApplicationService.EXPENSE_CLEARING_CODE)
                .orElseThrow();
        assertEquals("LIABILITY", clearing.getAccountKind());
        assertEquals("CREDIT", clearing.getNormalBalance());

        long clearingCredits = ledgerEntryRepository.findByLedgerAccountId(clearing.getId()).stream()
                .filter(e -> "CREDIT".equals(e.getEntryType()))
                .mapToLong(e -> e.getAmount())
                .sum();
        assertEquals(40_000L, clearingCredits);

        List<OutboxEvent> debits = outboxEventRepository.findAll().stream()
                .filter(e -> "WalletDebitPosted".equals(e.getEventType()))
                .filter(e -> first.id().equals(e.getAggregateId()))
                .toList();
        assertEquals(1, debits.size());
        assertEquals("EXPENSE_PAYMENT", debits.getFirst().getPayload().get("transactionType"));
    }

    @Test
    void insufficientFundsDoesNotPostExpense() {
        UUID familyId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        WalletResponse pool = createFamilyPool(familyId, "Expense-low");
        seedCredit(pool.id(), 5_000L, "exp-low");

        PermanentExpensePaymentException ex = assertThrows(
                PermanentExpensePaymentException.class,
                () -> walletApplicationService.payExpense(new ExpensePaymentCommand(
                        expenseId, familyId, pool.id(), 50_000L, "VND",
                        "expense:" + expenseId, "expense-pay:" + expenseId)));
        assertEquals("INSUFFICIENT_FUNDS", ex.getErrorCode());
        assertEquals(5_000L, walletApplicationService.getBalance(pool.id(), ACTOR).availableBalance());
        assertTrue(ledgerTransactionRepository.findByIdempotencyKey("expense-pay:" + expenseId).isEmpty());
        assertWalletProjectionMatchesJournal(pool.id());
    }

    @Test
    void crossFamilyOrInactiveWalletDoesNotPost() {
        UUID familyA = UUID.randomUUID();
        UUID familyB = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        WalletResponse poolB = createFamilyPool(familyB, "Other-family");
        seedCredit(poolB.id(), 100_000L, "other");

        PermanentExpensePaymentException cross = assertThrows(
                PermanentExpensePaymentException.class,
                () -> walletApplicationService.payExpense(new ExpensePaymentCommand(
                        expenseId, familyA, poolB.id(), 10_000L, "VND",
                        "expense:" + expenseId, "expense-pay:" + expenseId)));
        assertEquals("FAMILY_MISMATCH", cross.getErrorCode());

        WalletResponse inactive = walletApplicationService.createWallet(
                new CreateWalletRequest(familyA, null, "Inactive", WalletType.FAMILY_POOL), ACTOR);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            WalletAccount account = entityManager.find(WalletAccount.class, inactive.id());
            account.setStatus("FROZEN");
        });

        UUID expense2 = UUID.randomUUID();
        PermanentExpensePaymentException inactiveEx = assertThrows(
                PermanentExpensePaymentException.class,
                () -> walletApplicationService.payExpense(new ExpensePaymentCommand(
                        expense2, familyA, inactive.id(), 1_000L, "VND",
                        "expense:" + expense2, "expense-pay:" + expense2)));
        assertEquals("WALLET_INACTIVE", inactiveEx.getErrorCode());
        assertTrue(ledgerTransactionRepository.findByIdempotencyKey("expense-pay:" + expenseId).isEmpty());
        assertTrue(ledgerTransactionRepository.findByIdempotencyKey("expense-pay:" + expense2).isEmpty());
    }
}
