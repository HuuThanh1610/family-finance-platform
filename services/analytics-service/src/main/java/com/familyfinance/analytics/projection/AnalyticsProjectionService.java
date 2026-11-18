package com.familyfinance.analytics.projection;

import com.familyfinance.analytics.entity.*;
import com.familyfinance.analytics.messaging.DomainEventMessage;
import com.familyfinance.analytics.repository.*;
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
 * Projections are updated asynchronously via Kafka consumers; readers may observe
 * stale data for a short window (eventual consistency). Income/expense totals reconcile
 * after all events for a month are processed.
 */
@Service
public class AnalyticsProjectionService {

    private final FamilyMonthlySummaryRepository monthlySummaryRepository;
    private final CategorySpendingSummaryRepository categorySpendingSummaryRepository;
    private final MemberSpendingSummaryRepository memberSpendingSummaryRepository;
    private final BudgetCategoryActualRepository budgetCategoryActualRepository;
    private final WalletBalanceSummaryRepository walletBalanceSummaryRepository;

    public AnalyticsProjectionService(
            FamilyMonthlySummaryRepository monthlySummaryRepository,
            CategorySpendingSummaryRepository categorySpendingSummaryRepository,
            MemberSpendingSummaryRepository memberSpendingSummaryRepository,
            BudgetCategoryActualRepository budgetCategoryActualRepository,
            WalletBalanceSummaryRepository walletBalanceSummaryRepository) {
        this.monthlySummaryRepository = monthlySummaryRepository;
        this.categorySpendingSummaryRepository = categorySpendingSummaryRepository;
        this.memberSpendingSummaryRepository = memberSpendingSummaryRepository;
        this.budgetCategoryActualRepository = budgetCategoryActualRepository;
        this.walletBalanceSummaryRepository = walletBalanceSummaryRepository;
    }

    @Transactional
    public void apply(DomainEventMessage event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        Map<String, Object> payload = event.payload() != null ? event.payload() : Map.of();
        UUID familyId = uuid(payload.get("familyId"));
        if (familyId == null) {
            return;
        }
        Instant occurred = event.occurredAt() != null ? event.occurredAt() : Instant.now();
        LocalDate month = LocalDate.ofInstant(occurred, ZoneOffset.UTC).withDayOfMonth(1);

        switch (event.eventType()) {
            case "PaymentProviderPaid", "WalletCreditPosted" -> applyIncome(familyId, month, amount(payload), occurred);
            case "WalletDebitPosted" -> applyExpense(familyId, month, payload, occurred);
            case "ExpensePaymentRequested" -> applyExpense(familyId, month, payload, occurred);
            case "AllowanceDisbursementRequested" -> applyExpense(
                    familyId, month, withCategory(payload, "ALLOWANCE", "Tiền tiêu vặt"), occurred);
            case "InventoryLow" -> { /* inventory snapshot optional — count via household events later */ }
            default -> { }
        }
        applyWalletBalanceFromPayload(familyId, payload, occurred);
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

    private void applyExpense(UUID familyId, LocalDate month, Map<String, Object> payload, Instant now) {
        long amount = amount(payload);
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
                    .orElseGet(() -> newMember(familyId, memberKey, month, now));
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

    private static Map<String, Object> withCategory(Map<String, Object> payload, String code, String name) {
        return Map.of(
                "familyId", payload.get("familyId"),
                "amount", payload.get("amount"),
                "categoryCode", code,
                "categoryName", name,
                "requesterMemberId", payload.get("requesterMemberId")
        );
    }

    private static long amount(Map<String, Object> payload) {
        Object raw = payload.get("amount");
        if (raw == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(raw));
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
}