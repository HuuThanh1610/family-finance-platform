package com.familyfinance.wallet;

import com.familyfinance.wallet.dto.WalletTransferRequest;
import com.familyfinance.wallet.exception.InsufficientBalanceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletConcurrentTransferIT extends WalletIntegrationTestBase {

    @Test
    void concurrentTransfersCannotOverspend() throws Exception {
        UUID familyId = UUID.randomUUID();
        var source = createFamilyPool(familyId, "Source");
        var dest = createFamilyPool(familyId, "Dest");

        seedCredit(source.id(), 100_000L, UUID.randomUUID().toString());

        int threads = 20;
        long amountEach = 10_000L;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int idx = i;
            tasks.add(() -> {
                try {
                    walletApplicationService.transfer(new WalletTransferRequest(
                            source.id(),
                            dest.id(),
                            amountEach,
                            "xfer-" + idx + "-" + UUID.randomUUID(),
                            "idem-xfer-" + idx + "-" + UUID.randomUUID(),
                            "concurrent"
                    ), ACTOR);
                    success.incrementAndGet();
                } catch (InsufficientBalanceException ex) {
                    insufficient.incrementAndGet();
                    rejected.incrementAndGet();
                } catch (RuntimeException ex) {
                    rejected.incrementAndGet();
                }
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();

        long sourceBal = walletApplicationService.getBalance(source.id(), ACTOR).availableBalance();
        long destBal = walletApplicationService.getBalance(dest.id(), ACTOR).availableBalance();
        assertEquals(100_000L, sourceBal + destBal);
        assertTrue(sourceBal >= 0);
        assertEquals(10, success.get());
        assertEquals(10, rejected.get());
        assertTrue(insufficient.get() > 0, "some concurrent debits must fail with insufficient balance");
        assertWalletProjectionMatchesJournal(source.id());
        assertWalletProjectionMatchesJournal(dest.id());
    }

    @Test
    void concurrentOverlappingWalletTransfersPreserveConservation() throws Exception {
        UUID familyId = UUID.randomUUID();
        var walletA = createFamilyPool(familyId, "A");
        var walletB = createFamilyPool(familyId, "B");
        var walletC = createFamilyPool(familyId, "C");
        seedCredit(walletA.id(), 90_000L, UUID.randomUUID().toString());

        int threads = 12;
        long amount = 15_000L;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger rejected = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int idx = i;
            UUID destId = (idx % 2 == 0) ? walletB.id() : walletC.id();
            tasks.add(() -> {
                try {
                    walletApplicationService.transfer(new WalletTransferRequest(
                            walletA.id(),
                            destId,
                            amount,
                            "overlap-" + idx + "-" + UUID.randomUUID(),
                            "overlap-idem-" + idx + "-" + UUID.randomUUID(),
                            "overlap"), ACTOR);
                } catch (RuntimeException ex) {
                    rejected.incrementAndGet();
                }
                return null;
            });
        }
        pool.invokeAll(tasks).forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        pool.shutdown();

        long total = walletApplicationService.getBalance(walletA.id(), ACTOR).availableBalance()
                + walletApplicationService.getBalance(walletB.id(), ACTOR).availableBalance()
                + walletApplicationService.getBalance(walletC.id(), ACTOR).availableBalance();
        assertEquals(90_000L, total);
        assertTrue(rejected.get() > 0);
        assertWalletProjectionMatchesJournal(walletA.id());
        assertWalletProjectionMatchesJournal(walletB.id());
        assertWalletProjectionMatchesJournal(walletC.id());
    }

    @Test
    void concurrentBidirectionalTransfersOnSharedWalletsPreserveConservation() throws Exception {
        UUID familyId = UUID.randomUUID();
        var walletA = createFamilyPool(familyId, "A");
        var walletB = createFamilyPool(familyId, "B");
        seedCredit(walletA.id(), 80_000L, UUID.randomUUID().toString());
        seedCredit(walletB.id(), 40_000L, UUID.randomUUID().toString());

        long amount = 50_000L;
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> aToB = pool.submit(() -> {
                barrier.await();
                walletApplicationService.transfer(new WalletTransferRequest(
                        walletA.id(),
                        walletB.id(),
                        amount,
                        "ab-ref-" + UUID.randomUUID(),
                        "ab-idem-" + UUID.randomUUID(),
                        "A to B"), ACTOR);
                return null;
            });
            Future<?> bToA = pool.submit(() -> {
                barrier.await();
                walletApplicationService.transfer(new WalletTransferRequest(
                        walletB.id(),
                        walletA.id(),
                        amount,
                        "ba-ref-" + UUID.randomUUID(),
                        "ba-idem-" + UUID.randomUUID(),
                        "B to A"), ACTOR);
                return null;
            });
            for (Future<?> f : List.of(aToB, bToA)) {
                try {
                    f.get();
                } catch (Exception ex) {
                    // one direction may fail if balance is temporarily insufficient
                }
            }
        } finally {
            pool.shutdown();
        }

        long total = walletApplicationService.getBalance(walletA.id(), ACTOR).availableBalance()
                + walletApplicationService.getBalance(walletB.id(), ACTOR).availableBalance();
        assertEquals(120_000L, total);
        assertWalletProjectionMatchesJournal(walletA.id());
        assertWalletProjectionMatchesJournal(walletB.id());
    }

    @Test
    void concurrentSameIdempotencyKeyProducesSingleTransfer() throws Exception {
        UUID familyId = UUID.randomUUID();
        var source = createFamilyPool(familyId, "Source");
        var dest = createFamilyPool(familyId, "Dest");
        seedCredit(source.id(), 50_000L, UUID.randomUUID().toString());

        String referenceId = "shared-ref-" + UUID.randomUUID();
        String idempotencyKey = "shared-idem-" + UUID.randomUUID();
        long amount = 12_000L;
        int threads = 8;
        CyclicBarrier start = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger completed = new AtomicInteger();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                start.await();
                walletApplicationService.transfer(new WalletTransferRequest(
                        source.id(),
                        dest.id(),
                        amount,
                        referenceId,
                        idempotencyKey,
                        "same idempotency"), ACTOR);
                completed.incrementAndGet();
                return null;
            });
        }
        pool.invokeAll(tasks).forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        pool.shutdown();

        assertEquals(threads, completed.get());
        assertTrue(ledgerTransactionRepository.findByIdempotencyKey(idempotencyKey).isPresent());
        assertEquals(38_000L, walletApplicationService.getBalance(source.id(), ACTOR).availableBalance());
        assertEquals(12_000L, walletApplicationService.getBalance(dest.id(), ACTOR).availableBalance());
        assertWalletProjectionMatchesJournal(source.id());
        assertWalletProjectionMatchesJournal(dest.id());
    }

    @Test
    void journalProjectionMatchesBalancesAfterConcurrentLoad() throws Exception {
        UUID familyId = UUID.randomUUID();
        var source = createFamilyPool(familyId, "Source");
        var dest = createFamilyPool(familyId, "Dest");
        seedCredit(source.id(), 30_000L, UUID.randomUUID().toString());

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int idx = i;
            tasks.add(() -> {
                try {
                    walletApplicationService.transfer(new WalletTransferRequest(
                            source.id(),
                            dest.id(),
                            5_000L,
                            "proj-" + idx + "-" + UUID.randomUUID(),
                            "proj-idem-" + idx + "-" + UUID.randomUUID(),
                            "projection check"), ACTOR);
                } catch (RuntimeException ignored) {
                    // overspend attempts expected
                }
                return null;
            });
        }
        pool.invokeAll(tasks);
        pool.shutdown();

        assertWalletProjectionMatchesJournal(source.id());
        assertWalletProjectionMatchesJournal(dest.id());
        assertEquals(
                walletApplicationService.getBalance(source.id(), ACTOR).availableBalance()
                        + walletApplicationService.getBalance(dest.id(), ACTOR).availableBalance(),
                30_000L);
    }
}