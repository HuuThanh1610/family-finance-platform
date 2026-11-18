package com.familyfinance.wallet;

import com.familyfinance.wallet.domain.EntryType;
import com.familyfinance.wallet.dto.TopUpCreditRequest;
import com.familyfinance.wallet.dto.WalletTransferRequest;
import com.familyfinance.wallet.exception.BusinessException;
import com.familyfinance.wallet.exception.IdempotencyConflictException;
import com.familyfinance.wallet.service.LedgerPostingService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletLedgerIntegrityIT extends WalletIntegrationTestBase {

    @Test
    void duplicateLedgerReferenceRejected() {
        UUID familyId = UUID.randomUUID();
        var source = createFamilyPool(familyId, "Source");
        var dest = createFamilyPool(familyId, "Dest");
        seedCredit(source.id(), 50_000L, UUID.randomUUID().toString());

        String referenceId = "dup-ref-" + UUID.randomUUID();
        walletApplicationService.transfer(new WalletTransferRequest(
                source.id(), dest.id(), 10_000L, referenceId, "idem-a-" + UUID.randomUUID(), "first"), ACTOR);

        assertThrows(IdempotencyConflictException.class, () -> walletApplicationService.transfer(
                new WalletTransferRequest(
                        source.id(), dest.id(), 10_000L, referenceId, "idem-b-" + UUID.randomUUID(), "second"),
                ACTOR));

        assertEquals(40_000L, walletApplicationService.getBalance(source.id(), ACTOR).availableBalance());
        assertEquals(10_000L, walletApplicationService.getBalance(dest.id(), ACTOR).availableBalance());
    }

    @Test
    void unbalancedJournalRejected() {
        UUID familyId = UUID.randomUUID();
        var w1 = createFamilyPool(familyId, "W1");
        var w2 = createFamilyPool(familyId, "W2");
        seedCredit(w1.id(), 20_000L, UUID.randomUUID().toString());

        UUID liability1 = liabilityLedgerAccountId(w1.id());
        UUID liability2 = liabilityLedgerAccountId(w2.id());

        assertThrows(BusinessException.class, () -> ledgerPostingService.postBalancedTransaction(
                new LedgerPostingService.PostCommand(
                        familyId,
                        w1.id(),
                        "unbal-ref-" + UUID.randomUUID(),
                        "unbal-idem-" + UUID.randomUUID(),
                        "TEST_UNBALANCED",
                        "deliberately unbalanced",
                        List.of(
                                new LedgerPostingService.EntryLine(liability1, EntryType.DEBIT, 100, "out"),
                                new LedgerPostingService.EntryLine(liability2, EntryType.CREDIT, 50, "in")
                        ))));

        assertWalletProjectionMatchesJournal(w1.id());
        assertWalletProjectionMatchesJournal(w2.id());
    }

    @Test
    void creditFromPaymentDuplicateIdempotencyKeyIsIdempotent() {
        UUID familyId = UUID.randomUUID();
        var wallet = createFamilyPool(familyId, "TopUp");
        String ref = "topup-ref-" + UUID.randomUUID();
        String idem = "topup-idem-" + UUID.randomUUID();

        var first = walletApplicationService.creditFromPayment(new TopUpCreditRequest(
                wallet.id(), 25_000L, ref, idem, "first credit"));
        var second = walletApplicationService.creditFromPayment(new TopUpCreditRequest(
                wallet.id(), 25_000L, ref, idem, "retry credit"));

        assertEquals(first.id(), second.id());
        assertEquals(25_000L, walletApplicationService.getBalance(wallet.id(), ACTOR).availableBalance());
        assertTrue(ledgerTransactionRepository.findByIdempotencyKey(idem).isPresent());
        assertWalletProjectionMatchesJournal(wallet.id());
    }

    @Test
    void reverseTransactionRevertsTransferEffect() {
        UUID familyId = UUID.randomUUID();
        var source = createFamilyPool(familyId, "Source");
        var dest = createFamilyPool(familyId, "Dest");
        seedCredit(source.id(), 60_000L, UUID.randomUUID().toString());

        var transfer = walletApplicationService.transfer(new WalletTransferRequest(
                source.id(),
                dest.id(),
                15_000L,
                "xfer-ref-" + UUID.randomUUID(),
                "xfer-idem-" + UUID.randomUUID(),
                "before reverse"), ACTOR);

        assertEquals(45_000L, walletApplicationService.getBalance(source.id(), ACTOR).availableBalance());
        assertEquals(15_000L, walletApplicationService.getBalance(dest.id(), ACTOR).availableBalance());

        var reversal = walletApplicationService.reverse(
                transfer.id(),
                "rev-ref-" + UUID.randomUUID(),
                "rev-idem-" + UUID.randomUUID());

        assertNotNull(reversal.id());
        assertEquals(60_000L, walletApplicationService.getBalance(source.id(), ACTOR).availableBalance());
        assertEquals(0L, walletApplicationService.getBalance(dest.id(), ACTOR).availableBalance());
        assertWalletProjectionMatchesJournal(source.id());
        assertWalletProjectionMatchesJournal(dest.id());
    }
}