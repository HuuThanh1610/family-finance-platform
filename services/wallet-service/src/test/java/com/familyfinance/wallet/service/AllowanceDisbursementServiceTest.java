package com.familyfinance.wallet.service;

import com.familyfinance.wallet.domain.WalletType;
import com.familyfinance.wallet.dto.AllowanceDisbursementCommand;
import com.familyfinance.wallet.entity.LedgerAccount;
import com.familyfinance.wallet.entity.LedgerTransaction;
import com.familyfinance.wallet.entity.OutboxEvent;
import com.familyfinance.wallet.entity.WalletAccount;
import com.familyfinance.wallet.exception.PermanentAllowanceDisbursementException;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllowanceDisbursementServiceTest {

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
    UUID memberId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    UUID poolId = UUID.randomUUID();
    UUID memberWalletId = UUID.randomUUID();

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
    void disbursePostsInternalTransferAndCompletionOutbox() {
        stubWallets();
        LedgerTransaction tx = new LedgerTransaction();
        tx.setId(UUID.randomUUID());
        tx.setReferenceId("allowance:" + requestId);
        tx.setIdempotencyKey("allowance-disbursement:" + requestId);
        tx.setFamilyId(familyId);
        when(ledgerPostingService.postBalancedTransaction(any())).thenReturn(tx);
        when(outboxEventRepository.existsByEventTypeAndAggregateId(
                WalletApplicationService.EVENT_ALLOWANCE_COMPLETED, requestId)).thenReturn(false);

        service.disburseAllowance(new AllowanceDisbursementCommand(
                requestId, familyId, memberId, 50_000L, "VND"));

        ArgumentCaptor<LedgerPostingService.PostCommand> cmd =
                ArgumentCaptor.forClass(LedgerPostingService.PostCommand.class);
        verify(ledgerPostingService).postBalancedTransaction(cmd.capture());
        assertThat(cmd.getValue().idempotencyKey()).isEqualTo("allowance-disbursement:" + requestId);
        assertThat(cmd.getValue().transactionType()).isEqualTo("INTERNAL_TRANSFER");
        assertThat(cmd.getValue().entries()).hasSize(2);

        ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outbox.capture());
        assertThat(outbox.getValue().getEventType())
                .isEqualTo(WalletApplicationService.EVENT_ALLOWANCE_COMPLETED);
        assertThat(outbox.getValue().getPayload()).containsEntry("requestId", requestId.toString());
    }

    @Test
    void rejectsWrongFamilyMemberWallet() {
        WalletAccount pool = wallet(poolId, familyId, null, WalletType.FAMILY_POOL);
        WalletAccount foreign = wallet(memberWalletId, UUID.randomUUID(), memberId, WalletType.MEMBER_WALLET);
        when(walletAccountRepository.findByFamilyIdAndWalletTypeAndStatus(
                familyId, WalletType.FAMILY_POOL.name(), "ACTIVE")).thenReturn(List.of(pool));
        when(walletAccountRepository.findByFamilyIdAndOwnerMemberIdAndWalletTypeAndStatus(
                familyId, memberId, WalletType.MEMBER_WALLET.name(), "ACTIVE")).thenReturn(List.of(foreign));

        assertThatThrownBy(() -> service.disburseAllowance(new AllowanceDisbursementCommand(
                requestId, familyId, memberId, 10_000L, "VND")))
                .isInstanceOf(PermanentAllowanceDisbursementException.class)
                .extracting(ex -> ((PermanentAllowanceDisbursementException) ex).getErrorCode())
                .isEqualTo("FAMILY_MISMATCH");
        verify(ledgerPostingService, never()).postBalancedTransaction(any());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.disburseAllowance(new AllowanceDisbursementCommand(
                requestId, familyId, memberId, 0L, "VND")))
                .isInstanceOf(PermanentAllowanceDisbursementException.class)
                .extracting(ex -> ((PermanentAllowanceDisbursementException) ex).getErrorCode())
                .isEqualTo("INVALID_AMOUNT");
    }

    private void stubWallets() {
        WalletAccount pool = wallet(poolId, familyId, null, WalletType.FAMILY_POOL);
        WalletAccount member = wallet(memberWalletId, familyId, memberId, WalletType.MEMBER_WALLET);
        when(walletAccountRepository.findByFamilyIdAndWalletTypeAndStatus(
                familyId, WalletType.FAMILY_POOL.name(), "ACTIVE")).thenReturn(List.of(pool));
        when(walletAccountRepository.findByFamilyIdAndOwnerMemberIdAndWalletTypeAndStatus(
                familyId, memberId, WalletType.MEMBER_WALLET.name(), "ACTIVE")).thenReturn(List.of(member));
        when(walletAccountRepository.findByIdForUpdate(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.of(id.equals(poolId) ? pool : member);
        });
        LedgerAccount sourceLiab = liability(pool);
        LedgerAccount destLiab = liability(member);
        when(ledgerAccountRepository.findByWalletAccountIdAndAccountCode(poolId, WalletApplicationService.LIABILITY_CODE))
                .thenReturn(Optional.of(sourceLiab));
        when(ledgerAccountRepository.findByWalletAccountIdAndAccountCode(memberWalletId, WalletApplicationService.LIABILITY_CODE))
                .thenReturn(Optional.of(destLiab));
    }

    private WalletAccount wallet(UUID id, UUID family, UUID owner, WalletType type) {
        WalletAccount w = new WalletAccount();
        w.setId(id);
        w.setFamilyId(family);
        w.setOwnerMemberId(owner);
        w.setWalletType(type.name());
        w.setStatus("ACTIVE");
        w.setCurrencyCode("VND");
        w.setName(type.name());
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
}
