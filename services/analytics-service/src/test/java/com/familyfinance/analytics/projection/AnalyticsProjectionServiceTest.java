package com.familyfinance.analytics.projection;

import com.familyfinance.analytics.entity.FamilyMonthlySummary;
import com.familyfinance.analytics.messaging.DomainEventMessage;
import com.familyfinance.analytics.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsProjectionServiceTest {

    @Mock FamilyMonthlySummaryRepository monthlySummaryRepository;
    @Mock CategorySpendingSummaryRepository categorySpendingSummaryRepository;
    @Mock MemberSpendingSummaryRepository memberSpendingSummaryRepository;
    @Mock BudgetCategoryActualRepository budgetCategoryActualRepository;
    @Mock WalletBalanceSummaryRepository walletBalanceSummaryRepository;

    AnalyticsProjectionService service;

    UUID familyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AnalyticsProjectionService(
                monthlySummaryRepository,
                categorySpendingSummaryRepository,
                memberSpendingSummaryRepository,
                budgetCategoryActualRepository,
                walletBalanceSummaryRepository);
    }

    @Test
    void walletCreditIncreasesMonthlyIncome() {
        Instant occurred = Instant.parse("2026-11-15T10:00:00Z");
        LocalDate month = LocalDate.of(2026, 11, 1);

        when(monthlySummaryRepository.findByFamilyIdAndSummaryMonth(familyId, month)).thenReturn(Optional.empty());
        when(monthlySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.apply(new DomainEventMessage(
                UUID.randomUUID(),
                "WalletCreditPosted",
                "ledger_transaction",
                UUID.randomUUID(),
                null,
                occurred,
                Map.of("familyId", familyId.toString(), "amount", 1_000_000L)
        ));

        ArgumentCaptor<FamilyMonthlySummary> captor = ArgumentCaptor.forClass(FamilyMonthlySummary.class);
        verify(monthlySummaryRepository).save(captor.capture());
        assertThat(captor.getValue().getTotalIncome()).isEqualTo(1_000_000L);
        assertThat(captor.getValue().getSummaryMonth()).isEqualTo(month);
    }

    @Test
    void expensePaymentAddsCategoryAndBudgetActual() {
        Instant occurred = LocalDate.of(2026, 11, 18).atStartOfDay().toInstant(ZoneOffset.UTC);
        LocalDate month = LocalDate.of(2026, 11, 1);

        when(monthlySummaryRepository.findByFamilyIdAndSummaryMonth(familyId, month)).thenReturn(Optional.empty());
        when(categorySpendingSummaryRepository.findByFamilyIdAndCategoryCodeAndSummaryMonth(
                familyId, "FOOD", month)).thenReturn(Optional.empty());
        when(budgetCategoryActualRepository.findByFamilyIdAndCategoryCodeAndSummaryMonth(
                familyId, "FOOD", month)).thenReturn(Optional.empty());
        when(monthlySummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(categorySpendingSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(budgetCategoryActualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.apply(new DomainEventMessage(
                UUID.randomUUID(),
                "ExpensePaymentRequested",
                "expense",
                UUID.randomUUID(),
                null,
                occurred,
                Map.of(
                        "familyId", familyId.toString(),
                        "amount", 200_000,
                        "categoryCode", "FOOD",
                        "categoryName", "Ăn uống"
                )
        ));

        verify(categorySpendingSummaryRepository).save(any());
        verify(budgetCategoryActualRepository).save(any());
    }
}