package com.familyfinance.wallet.service;

import com.familyfinance.wallet.dto.ExpensePaymentCommand;
import com.familyfinance.wallet.entity.LedgerAccount;
import com.familyfinance.wallet.entity.LedgerTransaction;
import com.familyfinance.wallet.entity.WalletAccount;
import com.familyfinance.wallet.exception.PermanentExpensePaymentException;
import com.familyfinance.wallet.repository.LedgerAccountRepository;
import com.familyfinance.wallet.repository.LedgerEntryRepository;
import com.familyfinance.wallet.repository.LedgerTransactionRepository;
import com.familyfinance.wallet.repository.OutboxEventRepository;
import com.familyfinance.wallet.repository.WalletAccountRepository;
import com.familyfinance.wallet.repository.WalletBalanceRepository;
import com.familyfinance.wallet.security.FamilyAuthorizationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpensePaymentServiceTest {

    @Mock WalletAccountRepository walletAccountRepository;
    @Mock LedgerAccountRepository ledgerAccountRepository;
    @Mock WalletBalanceRepository walletBalanceRepository;
    @Mock LedgerTransactionRepository ledgerTransactionRepository;
    @Mock LedgerEntryRepository ledgerEntryRepository;
    @Mock LedgerPostingService ledgerPostingService;
    @Mock FamilyAuthorizationClient familyAuthorizationClient;
    @Mock OutboxEventRepository outboxEventRepository;

    WalletApplicationService service;

    UUID familyId = UUID.randomUUID();
    UUID walletId = UUID.randomUUID();
    UUID expenseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WalletApplicationService(
                walletAccountRepository,
                ledgerAccountRepository,
                walletBalanceRepository,
                ledgerTransactionRepository,
                ledgerEntryRepository,
                ledgerPostingService,
                familyAuthorizationClient,
                outboxEventRepository);
    }

    @Test
    void payExpensePostsExpensePaymentJournal() {
        WalletAccount wallet = wallet(walletId, familyId);
        when(walletAccountRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
        when(walletAccountRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        LedgerAccount liability = liability(wallet);
        LedgerAccount clearing = clearing();
        when(ledgerAccountRepository.findByWalletAccountIdAndAccountCode(walletId, WalletApplicationService.LIABILITY_CODE))
                .thenReturn(Optional.of(liability));
        when(ledgerAccountRepository.findByFamilyIdAndAccountCodeAndWalletAccountIsNull(
                familyId, WalletApplicationService.EXPENSE_CLEARING_CODE))
                .thenReturn(Optional.of(clearing));

        LedgerTransaction tx = new LedgerTransaction();
        tx.setId(UUID.randomUUID());
        tx.setReferenceId("expense:" + expenseId);
        tx.setIdempotencyKey("expense-pay:" + expenseId);
        tx.setFamilyId(familyId);
        when(ledgerPostingService.postBalancedTransaction(any())).thenReturn(tx);

        service.payExpense(cmd(50_000L));

        ArgumentCaptor<LedgerPostingService.PostCommand> captor =
                ArgumentCaptor.forClass(LedgerPostingService.PostCommand.class);
        verify(ledgerPostingService).postBalancedTransaction(captor.capture());
        assertThat(captor.getValue().transactionType()).isEqualTo("EXPENSE_PAYMENT");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("expense-pay:" + expenseId);
        assertThat(captor.getValue().entries()).hasSize(2);
    }

    @Test
    void crossFamilyWalletRejected() {
        WalletAccount wallet = wallet(walletId, UUID.randomUUID());
        when(walletAccountRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(wallet));
        when(walletAccountRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> service.payExpense(cmd(10_000L)))
                .isInstanceOf(PermanentExpensePaymentException.class)
                .extracting(ex -> ((PermanentExpensePaymentException) ex).getErrorCode())
                .isEqualTo("FAMILY_MISMATCH");
        verify(ledgerPostingService, never()).postBalancedTransaction(any());
    }

    private ExpensePaymentCommand cmd(long amount) {
        return new ExpensePaymentCommand(
                expenseId, familyId, walletId, amount, "VND",
                "expense:" + expenseId, "expense-pay:" + expenseId);
    }

    private WalletAccount wallet(UUID id, UUID family) {
        WalletAccount w = new WalletAccount();
        w.setId(id);
        w.setFamilyId(family);
        w.setStatus("ACTIVE");
        w.setCurrencyCode("VND");
        w.setWalletType("FAMILY_POOL");
        w.setName("pool");
        return w;
    }

    private LedgerAccount liability(WalletAccount wallet) {
        LedgerAccount a = new LedgerAccount();
        a.setId(UUID.randomUUID());
        a.setWalletAccount(wallet);
        a.setFamilyId(wallet.getFamilyId());
        a.setAccountCode(WalletApplicationService.LIABILITY_CODE);
        return a;
    }

    private LedgerAccount clearing() {
        LedgerAccount a = new LedgerAccount();
        a.setId(UUID.randomUUID());
        a.setFamilyId(familyId);
        a.setAccountCode(WalletApplicationService.EXPENSE_CLEARING_CODE);
        a.setAccountKind("LIABILITY");
        a.setNormalBalance("CREDIT");
        return a;
    }
}
