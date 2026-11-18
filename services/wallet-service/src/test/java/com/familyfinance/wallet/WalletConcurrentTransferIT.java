package com.familyfinance.wallet;

import com.familyfinance.wallet.domain.WalletType;
import com.familyfinance.wallet.dto.CreateWalletRequest;
import com.familyfinance.wallet.dto.TopUpCreditRequest;
import com.familyfinance.wallet.dto.WalletTransferRequest;
import com.familyfinance.wallet.service.WalletApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requires Docker. Enable with WALLET_IT=true.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "WALLET_IT", matches = "true")
class WalletConcurrentTransferIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("wallet_db")
            .withUsername("wallet_user")
            .withPassword("changeme_wallet");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("familyfinance.kafka.enabled", () -> "false");
    }

    @Autowired
    WalletApplicationService walletApplicationService;

    @Test
    void concurrentTransfersCannotOverspend() throws Exception {
        UUID familyId = UUID.randomUUID();
        var source = walletApplicationService.createWallet(new CreateWalletRequest(
                familyId, null, "Source", WalletType.FAMILY_POOL));
        var dest = walletApplicationService.createWallet(new CreateWalletRequest(
                familyId, null, "Dest", WalletType.FAMILY_POOL));

        walletApplicationService.creditFromPayment(new TopUpCreditRequest(
                source.id(), 100_000L, "seed-ref-" + UUID.randomUUID(), "seed-idem-" + UUID.randomUUID(), "seed"));

        int threads = 20;
        long amountEach = 10_000L;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
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
                    ));
                    success.incrementAndGet();
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

        long sourceBal = walletApplicationService.getBalance(source.id()).availableBalance();
        long destBal = walletApplicationService.getBalance(dest.id()).availableBalance();
        assertEquals(100_000L, sourceBal + destBal);
        assertTrue(sourceBal >= 0);
        assertEquals(10, success.get());
        assertEquals(10, rejected.get());
    }
}
