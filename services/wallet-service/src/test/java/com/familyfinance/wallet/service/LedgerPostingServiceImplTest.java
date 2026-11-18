package com.familyfinance.wallet.service;

import com.familyfinance.wallet.domain.EntryType;
import com.familyfinance.wallet.entity.LedgerAccount;
import com.familyfinance.wallet.entity.LedgerTransaction;
import com.familyfinance.wallet.entity.WalletAccount;
import com.familyfinance.wallet.entity.WalletBalance;
import com.familyfinance.wallet.exception.BusinessException;
import com.familyfinance.wallet.exception.InsufficientBalanceException;
import com.familyfinance.wallet.repository.LedgerAccountRepository;
import com.familyfinance.wallet.repository.LedgerEntryRepository;
import com.familyfinance.wallet.repository.LedgerTransactionRepository;
import com.familyfinance.wallet.repository.OutboxEventRepository;
import com.familyfinance.wallet.repository.WalletBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerPostingServiceImplTest {

    @Mock LedgerTransactionRepository transactionRepository;
    @Mock LedgerEntryRepository entryRepository;
    @Mock LedgerAccountRepository ledgerAccountRepository;
    @Mock WalletBalanceRepository balanceRepository;
    @Mock OutboxEventRepository outboxEventRepository;

    LedgerPostingServiceImpl service;

    UUID familyId = UUID.fromString("a1000000-0000-4000-8000-000000000001");
    UUID srcAccountId = UUID.fromString("e1000000-0000-4000-8000-000000000011");
    UUID dstAccountId = UUID.fromString("e1000000-0000-4000-8000-000000000012");

    @BeforeEach
    void setUp() {
        service = new LedgerPostingServiceImpl(
                transactionRepository,
                entryRepository,
                ledgerAccountRepository,
                balanceRepository,
                outboxEventRepository
        );
    }

    @Test
    void rejectsUnbalancedJournal() {
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.findByReferenceId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.postBalancedTransaction(new LedgerPostingService.PostCommand(
                familyId, null, "ref-1", "idem-1", "TRANSFER", "bad",
                List.of(
                        new LedgerPostingService.EntryLine(srcAccountId, EntryType.DEBIT, 100, null),
                        new LedgerPostingService.EntryLine(dstAccountId, EntryType.CREDIT, 50, null)
                )
        ))).isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unbalanced");
    }

    @Test
    void rejectsInsufficientBalance() {
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.findByReferenceId(any())).thenReturn(Optional.empty());

        LedgerAccount src = liability(srcAccountId);
        LedgerAccount dst = liability(dstAccountId);
        when(ledgerAccountRepository.findById(srcAccountId)).thenReturn(Optional.of(src));
        when(ledgerAccountRepository.findById(dstAccountId)).thenReturn(Optional.of(dst));

        WalletBalance srcBal = balance(src, 100L);
        WalletBalance dstBal = balance(dst, 0L);
        when(balanceRepository.findByLedgerAccountIdForUpdate(srcAccountId)).thenReturn(Optional.of(srcBal));
        when(balanceRepository.findByLedgerAccountIdForUpdate(dstAccountId)).thenReturn(Optional.of(dstBal));

        assertThatThrownBy(() -> service.postBalancedTransaction(new LedgerPostingService.PostCommand(
                familyId, null, "ref-2", "idem-2", "TRANSFER", "overspend",
                List.of(
                        new LedgerPostingService.EntryLine(srcAccountId, EntryType.DEBIT, 500, null),
                        new LedgerPostingService.EntryLine(dstAccountId, EntryType.CREDIT, 500, null)
                )
        ))).isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void postsBalancedTransferAndUpdatesBalances() {
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.findByReferenceId(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            LedgerTransaction tx = inv.getArgument(0);
            tx.setId(UUID.randomUUID());
            return tx;
        });

        LedgerAccount src = liability(srcAccountId);
        LedgerAccount dst = liability(dstAccountId);
        when(ledgerAccountRepository.findById(srcAccountId)).thenReturn(Optional.of(src));
        when(ledgerAccountRepository.findById(dstAccountId)).thenReturn(Optional.of(dst));

        WalletBalance srcBal = balance(src, 1000L);
        WalletBalance dstBal = balance(dst, 0L);
        when(balanceRepository.findByLedgerAccountIdForUpdate(srcAccountId)).thenReturn(Optional.of(srcBal));
        when(balanceRepository.findByLedgerAccountIdForUpdate(dstAccountId)).thenReturn(Optional.of(dstBal));

        LedgerTransaction result = service.postBalancedTransaction(new LedgerPostingService.PostCommand(
                familyId, null, "ref-ok", "idem-ok", "INTERNAL_TRANSFER", "ok",
                List.of(
                        new LedgerPostingService.EntryLine(srcAccountId, EntryType.DEBIT, 400, "out"),
                        new LedgerPostingService.EntryLine(dstAccountId, EntryType.CREDIT, 400, "in")
                )
        ));

        assertThat(result.getId()).isNotNull();
        assertThat(srcBal.getBalanceAmount()).isEqualTo(600L);
        assertThat(dstBal.getBalanceAmount()).isEqualTo(400L);
        verify(outboxEventRepository).save(any());
        ArgumentCaptor<LedgerTransaction> captor = ArgumentCaptor.forClass(LedgerTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getEntries()).hasSize(2);
    }

    private static LedgerAccount liability(UUID id) {
        LedgerAccount a = new LedgerAccount();
        a.setId(id);
        a.setAccountCode("WALLET_LIABILITY");
        a.setAccountKind("LIABILITY");
        a.setNormalBalance("CREDIT");
        a.setName("liability");
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        WalletAccount w = new WalletAccount();
        w.setId(UUID.randomUUID());
        a.setWalletAccount(w);
        return a;
    }

    private static WalletBalance balance(LedgerAccount account, long amount) {
        WalletBalance b = new WalletBalance();
        b.setId(UUID.randomUUID());
        b.setLedgerAccount(account);
        b.setBalanceAmount(amount);
        b.setVersion(0);
        b.setRecalculatedAt(Instant.now());
        b.setUpdatedAt(Instant.now());
        return b;
    }
}
