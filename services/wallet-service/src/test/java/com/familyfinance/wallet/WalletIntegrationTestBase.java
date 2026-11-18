package com.familyfinance.wallet;

import com.familyfinance.wallet.domain.WalletType;
import com.familyfinance.wallet.dto.CreateWalletRequest;
import com.familyfinance.wallet.dto.TopUpCreditRequest;
import com.familyfinance.wallet.dto.WalletResponse;
import com.familyfinance.wallet.repository.LedgerAccountRepository;
import com.familyfinance.wallet.repository.LedgerEntryRepository;
import com.familyfinance.wallet.repository.LedgerTransactionRepository;
import com.familyfinance.wallet.repository.WalletBalanceRepository;
import com.familyfinance.wallet.service.LedgerPostingService;
import com.familyfinance.wallet.service.WalletApplicationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Wallet integration tests against real PostgreSQL.
 * <p>
 * Enable with {@code WALLET_IT=true}. Prefers Testcontainers; if the local Docker
 * Engine API is incompatible with docker-java (seen with some Docker Desktop 29 builds),
 * set {@code WALLET_IT_JDBC_URL} (and optional user/password) to an existing Postgres
 * such as the compose {@code wallet_db}.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "WALLET_IT", matches = "true")
abstract class WalletIntegrationTestBase {

    protected static final UUID ACTOR = UUID.fromString("b1000000-0000-4000-8000-000000000001");

    private static final String EXTERNAL_JDBC = System.getenv("WALLET_IT_JDBC_URL");
    private static final boolean USE_EXTERNAL = EXTERNAL_JDBC != null && !EXTERNAL_JDBC.isBlank();

    private static PostgreSQLContainer<?> postgres;

    static {
        if (!USE_EXTERNAL) {
            try {
                DockerClientFactory.instance().client();
                postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("wallet_db")
                        .withUsername("wallet_user")
                        .withPassword("changeme_wallet");
                postgres.start();
            } catch (Throwable ex) {
                throw new IllegalStateException(
                        "Testcontainers Docker unavailable. Re-run with WALLET_IT_JDBC_URL="
                                + "jdbc:postgresql://localhost:5432/wallet_db (compose wallet_db). Cause: "
                                + ex.getMessage(),
                        ex);
            }
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        if (USE_EXTERNAL) {
            registry.add("spring.datasource.url", () -> EXTERNAL_JDBC);
            registry.add("spring.datasource.username",
                    () -> envOr("WALLET_IT_DB_USER", "wallet_user"));
            registry.add("spring.datasource.password",
                    () -> envOr("WALLET_IT_DB_PASSWORD", "changeme_wallet"));
        } else {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
        registry.add("familyfinance.kafka.enabled", () -> "false");
        registry.add("familyfinance.family-auth.enabled", () -> "false");
        registry.add("familyfinance.security.enabled", () -> "false");
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    @Autowired
    protected WalletApplicationService walletApplicationService;

    @Autowired
    protected LedgerPostingService ledgerPostingService;

    @Autowired
    protected LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    protected LedgerTransactionRepository ledgerTransactionRepository;

    @Autowired
    protected LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    protected WalletBalanceRepository walletBalanceRepository;

    @Autowired
    protected EntityManager entityManager;

    protected WalletResponse createFamilyPool(UUID familyId, String name) {
        return walletApplicationService.createWallet(
                new CreateWalletRequest(familyId, null, name, WalletType.FAMILY_POOL), ACTOR);
    }

    protected void seedCredit(UUID walletId, long amount, String suffix) {
        walletApplicationService.creditFromPayment(new TopUpCreditRequest(
                walletId,
                amount,
                "seed-ref-" + suffix + "-" + UUID.randomUUID(),
                "seed-idem-" + suffix + "-" + UUID.randomUUID(),
                "seed"));
    }

    protected UUID liabilityLedgerAccountId(UUID walletId) {
        return ledgerAccountRepository
                .findByWalletAccountIdAndAccountCode(walletId, WalletApplicationService.LIABILITY_CODE)
                .orElseThrow()
                .getId();
    }

    protected void assertWalletProjectionMatchesJournal(UUID walletId) {
        entityManager.clear();
        long projected = walletApplicationService.getBalance(walletId, ACTOR).availableBalance();
        UUID liabilityId = liabilityLedgerAccountId(walletId);
        long journal = ledgerEntryRepository.findByLedgerAccountId(liabilityId).stream()
                .mapToLong(e -> "CREDIT".equals(e.getEntryType()) ? e.getAmount() : -e.getAmount())
                .sum();
        assertEquals(journal, projected, "wallet_balances must match immutable journal for " + walletId);
    }
}
