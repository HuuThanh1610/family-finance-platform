package com.familyfinance.wallet;

import com.familyfinance.wallet.domain.WalletType;
import com.familyfinance.wallet.dto.AllowanceDisbursementCommand;
import com.familyfinance.wallet.dto.CreateWalletRequest;
import com.familyfinance.wallet.dto.LedgerTransactionResponse;
import com.familyfinance.wallet.dto.WalletResponse;
import com.familyfinance.wallet.entity.OutboxEvent;
import com.familyfinance.wallet.exception.PermanentAllowanceDisbursementException;
import com.familyfinance.wallet.repository.OutboxEventRepository;
import com.familyfinance.wallet.service.WalletApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletAllowanceDisbursementIT extends WalletIntegrationTestBase {

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Test
    void allowanceDisbursementTransfersOnceAndWritesCompletionOutbox() {
        UUID familyId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        WalletResponse pool = createFamilyPool(familyId, "Pool");
        WalletResponse memberWallet = walletApplicationService.createWallet(
                new CreateWalletRequest(familyId, memberId, "Child", WalletType.MEMBER_WALLET), ACTOR);
        seedCredit(pool.id(), 200_000L, "allow-seed");

        AllowanceDisbursementCommand cmd = new AllowanceDisbursementCommand(
                requestId, familyId, memberId, 75_000L, "VND");

        LedgerTransactionResponse first = walletApplicationService.disburseAllowance(cmd);
        LedgerTransactionResponse second = walletApplicationService.disburseAllowance(cmd);

        assertEquals(first.id(), second.id());
        assertEquals(125_000L, walletApplicationService.getBalance(pool.id(), ACTOR).availableBalance());
        assertEquals(75_000L, walletApplicationService.getBalance(memberWallet.id(), ACTOR).availableBalance());
        assertWalletProjectionMatchesJournal(pool.id());
        assertWalletProjectionMatchesJournal(memberWallet.id());

        List<OutboxEvent> completions = outboxEventRepository.findAll().stream()
                .filter(e -> WalletApplicationService.EVENT_ALLOWANCE_COMPLETED.equals(e.getEventType()))
                .filter(e -> requestId.equals(e.getAggregateId()))
                .toList();
        assertEquals(1, completions.size());
        assertEquals("PENDING", completions.getFirst().getStatus());
        assertTrue(completions.getFirst().getPayload().get("requestId").toString().equals(requestId.toString()));
    }

    @Test
    void insufficientFundsDoesNotPostTransfer() {
        UUID familyId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        WalletResponse pool = createFamilyPool(familyId, "Pool-low");
        walletApplicationService.createWallet(
                new CreateWalletRequest(familyId, memberId, "Child-low", WalletType.MEMBER_WALLET), ACTOR);
        seedCredit(pool.id(), 10_000L, "allow-low");

        PermanentAllowanceDisbursementException ex = assertThrows(
                PermanentAllowanceDisbursementException.class,
                () -> walletApplicationService.disburseAllowance(new AllowanceDisbursementCommand(
                        requestId, familyId, memberId, 50_000L, "VND")));
        assertEquals("INSUFFICIENT_FUNDS", ex.getErrorCode());
        assertEquals(10_000L, walletApplicationService.getBalance(pool.id(), ACTOR).availableBalance());
        assertWalletProjectionMatchesJournal(pool.id());
        assertTrue(ledgerTransactionRepository.findByIdempotencyKey(
                WalletApplicationService.IDEMPOTENCY_ALLOWANCE_PREFIX + requestId).isEmpty());
    }
}
