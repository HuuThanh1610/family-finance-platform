package com.familyfinance.analytics.service;

import com.familyfinance.analytics.entity.FamilyMonthlySummary;
import com.familyfinance.analytics.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsReportServiceTest {

    @Mock FamilyMonthlySummaryRepository monthlySummaryRepository;
    @Mock CategorySpendingSummaryRepository categorySpendingSummaryRepository;
    @Mock MemberSpendingSummaryRepository memberSpendingSummaryRepository;
    @Mock BudgetCategoryActualRepository budgetCategoryActualRepository;

    AnalyticsReportService service;

    UUID familyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AnalyticsReportService(
                monthlySummaryRepository,
                categorySpendingSummaryRepository,
                memberSpendingSummaryRepository,
                budgetCategoryActualRepository);
    }

    @Test
    void buildsMonthlyTrendsFromReadModel() {
        FamilyMonthlySummary nov = new FamilyMonthlySummary();
        nov.setFamilyId(familyId);
        nov.setSummaryMonth(LocalDate.of(2026, 11, 1));
        nov.setTotalIncome(5_000_000L);
        nov.setTotalExpense(2_000_000L);
        nov.setNetCashflow(3_000_000L);
        nov.setGeneratedAt(Instant.now());
        nov.setCreatedAt(Instant.now());
        nov.setUpdatedAt(Instant.now());

        when(monthlySummaryRepository.findByFamilyIdOrderBySummaryMonthAsc(familyId)).thenReturn(List.of(nov));
        when(categorySpendingSummaryRepository.findByFamilyIdAndSummaryMonth(eq(familyId), any(LocalDate.class)))
                .thenReturn(List.of());
        when(memberSpendingSummaryRepository.findByFamilyIdAndSummaryMonth(eq(familyId), any(LocalDate.class)))
                .thenReturn(List.of());
        when(budgetCategoryActualRepository.findByFamilyIdAndSummaryMonthOrderByCategoryCodeAsc(
                eq(familyId), any(LocalDate.class))).thenReturn(List.of());

        var report = service.buildReport(familyId);

        assertThat(report.monthlyTrends()).hasSize(1);
        assertThat(report.monthlyTrends().getFirst().income()).isEqualTo(5_000_000L);
    }

}