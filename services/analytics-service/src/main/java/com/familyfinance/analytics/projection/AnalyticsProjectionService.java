package com.familyfinance.analytics.projection;

import com.familyfinance.analytics.entity.*;
import com.familyfinance.analytics.messaging.DomainEventMessage;
import com.familyfinance.analytics.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

/**
 * CQRS read-model projector for analytics_db.
 * <p>
 * Realized income/expense are driven only by authoritative Wallet ledger postings:
 * <ul>
 *   <li>Income: {@code WalletCreditPosted} with {@code transactionType=PAYMENT_TOPUP}</li>
 *   <li>Expense: {@code WalletDebitPosted} with {@code transactionType=EXPENSE_PAYMENT}</li>
 * </ul>
 * Intent/request/failure events and internal transfers do not mutate realized cashflow.
 * Business idempotency uses {@code ledger_transaction_id} (one posting → one cashflow effect).
 */
@Service
public class AnalyticsProjectionService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsProjectionService.class);

    public static final String TX_PAYMENT_TOPUP = "PAYMENT_TOPUP";
    public static final String TX_EXPENSE_PAYMENT = "EXPENSE_PAYMENT";
    public static final String TX_INTERNAL_TRANSFER = "INTERNAL_TRANSFER";
    public static final String KIND_INCOME = "INCOME";
    public static final String KIND_EXPENSE = "EXPENSE";

    private final FamilyMonthlySummaryRepository monthlySummaryRepository;
    private final CategorySpendingSummaryRepository categorySpendingSummaryRepository;
    private final MemberSpendingSummaryRepository memberSpendingSummaryRepository;
    private final BudgetCategoryActualRepository budgetCategoryActualRepository;
    private final WalletBalanceSummaryRepository walletBalanceSummaryRepository;
    private final RealizedFinancialPostingRepository realizedFinancialPostingRepository;

    public AnalyticsProjectionService(
            FamilyMonthlySummaryRepository monthlySummaryRepository,
            CategorySpendingSummaryRepository categorySpendingSummaryRepository,
            MemberSpendingSummaryRepository memberSpendingSummaryRepository,
            BudgetCategoryActualRepository budgetCategoryActualRepository,
            WalletBalanceSummaryRepository walletBalanceSummaryRepository,
            RealizedFinancialPostingRepository realizedFinancialPostingRepository) {
        this.monthlySummaryRepository = monthlySummaryRepository;
        this.categorySpendingSummaryRepository = categorySpendingSummaryRepository;
        this.memberSpendingSummaryRepository = memberSpendingSummaryRepository;
        this.budgetCategoryActualRepository = budgetCategoryActualRepository;
        this.walletBalanceSummaryRepository = walletBalanceSummaryRepository;
        this.realizedFinancialPostingRepository = realizedFinancialPostingRepository;
    }

    @Transactional
    public void apply(DomainEventMessage event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        UUID familyId = uuid(payload.get("familyId"));
        if (familyId == null) {
            log.warn("Skipping analytics event without familyId eventType={} eventId={}",
                    event.eventType(), event.eventId());
            return;
        }
        Instant occurred = event.occurredAt() != null ? event.occurredAt() : Instant.now();
        LocalDate month = LocalDate.ofInstant(occurred, ZoneOffset.UTC).withDayOfMonth(1);

        switch (event.eventType()) {
            case "WalletCreditPosted" -> applyAuthoritativeIncome(event, familyId, month, payload, occurred);
            case "WalletDebitPosted" -> applyAuthoritativeExpense(event, familyId, month, payload, occurred);
            case "WalletTransferPosted" -> logIgnoredTransfer(event, payload);
            case "PaymentProviderPaid",
                 "ExpensePaymentRequested",
                 "ExpensePaymentFailed",
                 "AllowanceDisbursementRequested",
                 "AllowanceDisbursementCompleted",
                 "AllowanceDisbursementFailed" ->
                    log.debug("Ignoring non-realized eventType={} eventId={}", event.eventType(), event.eventId());
            case "InventoryLow" -> { /* inventory snapshot optional */ }
            default -> log.info("Unknown analytics eventType={} eventId={} — no cashflow mutation",
                    event.eventType(), event.eventId());
        }
        applyWalletBalanceFromPayload(familyId, payload, occurred);
    }

    private void applyAuthoritativeIncome(
            DomainEventMessage event,
            UUID familyId,
            LocalDate month,
            Map<String, Object> payload,
            Instant now) {
        String txType = string(payload.get("transactionType"), TX_PAYMENT_TOPUP);
        if (!TX_PAYMENT_TOPUP.equals(txType)) {
            log.info("Quarantining WalletCreditPosted with unsupported transactionType={} eventId={}",
                    txType, event.eventId());
            return;
        }
        ValidatedPosting posting = validatePosting(event, familyId, payload);
        if (posting == null) {
            return;
        }
        if (!claimPosting(posting, KIND_INCOME)) {
            return;
        }
        applyIncome(familyId, month, posting.amount(), now);
    }

    private void applyAuthoritativeExpense(
            DomainEventMessage event,
            UUID familyId,
            LocalDate month,
            Map<String, Object> payload,
            Instant now) {
        String txType = string(payload.get("transactionType"), "");
        if (TX_INTERNAL_TRANSFER.equals(txType)) {
            logIgnoredTransfer(event, payload);
            return;
        }
        if (!TX_EXPENSE_PAYMENT.equals(txType)) {
            log.info("Quarantining WalletDebitPosted with unsupported transactionType={} eventId={}",
                    txType, event.eventId());
            return;
        }
        ValidatedPosting posting = validatePosting(event, familyId, payload);
        if (posting == null) {
            return;
        }
        if (!claimPosting(posting, KIND_EXPENSE)) {
            return;
        }
        applyExpense(familyId, month, payload, posting.amount(), now);
    }

    private ValidatedPosting validatePosting(DomainEventMessage event, UUID familyId, Map<String, Object> payload) {
        UUID ledgerTxId = uuid(payload.get("transactionId"));
        if (ledgerTxId == null) {
            ledgerTxId = event.aggregateId();
        }
        if (ledgerTxId == null) {
            log.warn("Skipping posting without ledger transactionId eventType={} eventId={}",
                    event.eventType(), event.eventId());
            return null;
        }
        long amount = amount(payload);
        if (amount <= 0) {
            log.warn("Skipping posting with invalid amount={} eventType={} eventId={}",
                    amount, event.eventType(), event.eventId());
            return null;
        }
        String currency = string(payload.get("currencyCode"), "VND");
        if (currency.length() != 3) {
            log.warn("Skipping posting with invalid currency={} eventType={} eventId={}",
                    currency, event.eventType(), event.eventId());
            return null;
        }
        String referenceId = string(payload.get("referenceId"), null);
        return new ValidatedPosting(familyId, ledgerTxId, referenceId, amount, currency, event.eventId());
    }

    private boolean claimPosting(ValidatedPosting posting, String kind) {
        if (realizedFinancialPostingRepository.existsByLedgerTransactionId(posting.ledgerTransactionId())) {
            log.info("Duplicate ledger posting skipped ledgerTransactionId={} kind={}",
                    posting.ledgerTransactionId(), kind);
            return false;
        }
        RealizedFinancialPosting row = new RealizedFinancialPosting();
        row.setFamilyId(posting.familyId());
        row.setLedgerTransactionId(posting.ledgerTransactionId());
        row.setReferenceId(posting.referenceId());
        row.setPostingKind(kind);
        row.setAmount(posting.amount());
        row.setCurrencyCode(posting.currencyCode());
        row.setEventId(posting.eventId());
        row.setCreatedAt(Instant.now());
        try {
            realizedFinancialPostingRepository.saveAndFlush(row);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent duplicate ledger posting skipped ledgerTransactionId={}",
                    posting.ledgerTransactionId());
            return false;
        }
    }

    private void logIgnoredTransfer(DomainEventMessage event, Map<String, Object> payload) {
        log.debug(
                "Internal transfer ignored for realized cashflow eventType={} transactionType={} eventId={}",
                event.eventType(),
                payload.get("transactionType"),
                event.eventId());
    }

    private void applyIncome(UUID familyId, LocalDate month, long amount, Instant now) {
        FamilyMonthlySummary row = monthlySummaryRepository.findByFamilyIdAndSummaryMonth(familyId, month)
                .orElseGet(() -> newSummary(familyId, month, now));
        row.setTotalIncome(row.getTotalIncome() + amount);
        row.setNetCashflow(row.getTotalIncome() - row.getTotalExpense());
        row.setGeneratedAt(now);
        row.setUpdatedAt(now);
        monthlySummaryRepository.save(row);
    }

    private void applyExpense(UUID familyId, LocalDate month, Map<String, Object> payload, long amount, Instant now) {
        FamilyMonthlySummary row = monthlySummaryRepository.findByFamilyIdAndSummaryMonth(familyId, month)
                .orElseGet(() -> newSummary(familyId, month, now));
        row.setTotalExpense(row.getTotalExpense() + amount);
        row.setNetCashflow(row.getTotalIncome() - row.getTotalExpense());
        row.setGeneratedAt(now);
        row.setUpdatedAt(now);
        monthlySummaryRepository.save(row);

        String categoryCode = string(payload.get("categoryCode"), "OTHER");
        CategorySpendingSummary category = categorySpendingSummaryRepository
                .findByFamilyIdAndCategoryCodeAndSummaryMonth(familyId, categoryCode, month)
                .orElseGet(() -> newCategory(familyId, categoryCode, month, now));
        category.setTotalSpent(category.getTotalSpent() + amount);
        category.setGeneratedAt(now);
        category.setUpdatedAt(now);
        categorySpendingSummaryRepository.save(category);

        UUID paidBy = uuid(payload.get("paidByMemberId"));
        UUID requester = uuid(payload.get("requesterMemberId"));
        final UUID memberId = paidBy != null ? paidBy : requester;
        if (memberId != null) {
            MemberSpendingSummary member = memberSpendingSummaryRepository
                    .findByFamilyIdAndMemberIdAndSummaryMonth(familyId, memberId, month)
                    .orElseGet(() -> newMember(familyId, memberId, month, now));
            member.setTotalSpent(member.getTotalSpent() + amount);
            member.setTransactionCount(member.getTransactionCount() + 1);
            member.setGeneratedAt(now);
            member.setUpdatedAt(now);
            memberSpendingSummaryRepository.save(member);
        }

        String categoryName = string(payload.get("categoryName"), categoryCode);
        BudgetCategoryActual budget = budgetCategoryActualRepository
                .findByFamilyIdAndCategoryCodeAndSummaryMonth(familyId, categoryCode, month)
                .orElseGet(() -> newBudgetCategory(familyId, categoryCode, categoryName, month, now));
        budget.setActualAmount(budget.getActualAmount() + amount);
        budget.setGeneratedAt(now);
        budget.setUpdatedAt(now);
        budgetCategoryActualRepository.save(budget);
    }

    private void applyWalletBalanceFromPayload(UUID familyId, Map<String, Object> payload, Instant now) {
        UUID walletId = uuid(payload.get("walletId"));
        if (walletId == null) {
            return;
        }
        Long balance = longOrNull(payload.get("balanceAfter"));
        if (balance == null) {
            balance = longOrNull(payload.get("balance"));
        }
        if (balance == null) {
            return;
        }
        WalletBalanceSummary row = walletBalanceSummaryRepository.findByFamilyIdAndWalletId(familyId, walletId)
                .orElseGet(() -> {
                    WalletBalanceSummary created = new WalletBalanceSummary();
                    created.setFamilyId(familyId);
                    created.setWalletId(walletId);
                    created.setCreatedAt(now);
                    return created;
                });
        row.setBalanceAmount(balance);
        row.setSnapshotAt(now);
        row.setUpdatedAt(now);
        walletBalanceSummaryRepository.save(row);
    }

    private static FamilyMonthlySummary newSummary(UUID familyId, LocalDate month, Instant now) {
        FamilyMonthlySummary s = new FamilyMonthlySummary();
        s.setFamilyId(familyId);
        s.setSummaryMonth(month);
        s.setCreatedAt(now);
        s.setGeneratedAt(now);
        s.setUpdatedAt(now);
        return s;
    }

    private static CategorySpendingSummary newCategory(UUID familyId, String code, LocalDate month, Instant now) {
        CategorySpendingSummary s = new CategorySpendingSummary();
        s.setFamilyId(familyId);
        s.setCategoryCode(code);
        s.setSummaryMonth(month);
        s.setCreatedAt(now);
        s.setGeneratedAt(now);
        s.setUpdatedAt(now);
        return s;
    }

    private static MemberSpendingSummary newMember(UUID familyId, UUID memberId, LocalDate month, Instant now) {
        MemberSpendingSummary s = new MemberSpendingSummary();
        s.setFamilyId(familyId);
        s.setMemberId(memberId);
        s.setSummaryMonth(month);
        s.setCreatedAt(now);
        s.setGeneratedAt(now);
        s.setUpdatedAt(now);
        return s;
    }

    private static BudgetCategoryActual newBudgetCategory(
            UUID familyId, String code, String name, LocalDate month, Instant now) {
        BudgetCategoryActual s = new BudgetCategoryActual();
        s.setFamilyId(familyId);
        s.setCategoryCode(code);
        s.setCategoryName(name);
        s.setSummaryMonth(month);
        s.setCreatedAt(now);
        s.setGeneratedAt(now);
        s.setUpdatedAt(now);
        return s;
    }

    private static long amount(Map<String, Object> payload) {
        Object raw = payload.get("amount");
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static UUID uuid(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String string(Object raw, String fallback) {
        return raw == null ? fallback : String.valueOf(raw);
    }

    private static Long longOrNull(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record ValidatedPosting(
            UUID familyId,
            UUID ledgerTransactionId,
            String referenceId,
            long amount,
            String currencyCode,
            UUID eventId) {
    }
}
