package com.familyfinance.analytics.service;

import com.familyfinance.analytics.dto.AnalyticsReportResponse;
import com.familyfinance.analytics.dto.AnalyticsReportResponse.*;
import com.familyfinance.analytics.entity.BudgetCategoryActual;
import com.familyfinance.analytics.entity.FamilyMonthlySummary;
import com.familyfinance.analytics.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AnalyticsReportService {

    private static final int TREND_MONTHS = 6;
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM", new Locale("vi", "VN"));

    private final FamilyMonthlySummaryRepository monthlySummaryRepository;
    private final CategorySpendingSummaryRepository categorySpendingSummaryRepository;
    private final MemberSpendingSummaryRepository memberSpendingSummaryRepository;
    private final BudgetCategoryActualRepository budgetCategoryActualRepository;

    public AnalyticsReportService(
            FamilyMonthlySummaryRepository monthlySummaryRepository,
            CategorySpendingSummaryRepository categorySpendingSummaryRepository,
            MemberSpendingSummaryRepository memberSpendingSummaryRepository,
            BudgetCategoryActualRepository budgetCategoryActualRepository) {
        this.monthlySummaryRepository = monthlySummaryRepository;
        this.categorySpendingSummaryRepository = categorySpendingSummaryRepository;
        this.memberSpendingSummaryRepository = memberSpendingSummaryRepository;
        this.budgetCategoryActualRepository = budgetCategoryActualRepository;
    }

    public AnalyticsReportResponse buildReport(UUID familyId) {
        List<FamilyMonthlySummary> months = monthlySummaryRepository.findByFamilyIdOrderBySummaryMonthAsc(familyId);
        List<MonthlyTrendPoint> trends = months.stream()
                .sorted(Comparator.comparing(FamilyMonthlySummary::getSummaryMonth))
                .skip(Math.max(0, months.size() - TREND_MONTHS))
                .map(m -> new MonthlyTrendPoint(
                        "T" + m.getSummaryMonth().getMonthValue(),
                        m.getTotalIncome(),
                        m.getTotalExpense()))
                .toList();

        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        var categories = categorySpendingSummaryRepository.findByFamilyIdAndSummaryMonth(familyId, currentMonth);
        List<CategorySpendPoint> byCategory = categories.stream()
                .map(c -> new CategorySpendPoint(c.getCategoryCode(), c.getTotalSpent()))
                .toList();

        var members = memberSpendingSummaryRepository.findByFamilyIdAndSummaryMonth(familyId, currentMonth);
        List<MemberSpendPoint> byMember = members.stream()
                .map(m -> new MemberSpendPoint(shortMemberName(m.getMemberId()), m.getTotalSpent()))
                .toList();

        List<BudgetCategoryActual> budgetRows =
                budgetCategoryActualRepository.findByFamilyIdAndSummaryMonthOrderByCategoryCodeAsc(
                        familyId, currentMonth);
        long totalAllocated = 0;
        long totalSpent = 0;
        List<BudgetCategoryPoint> budgetCategories = new ArrayList<>();
        for (BudgetCategoryActual row : budgetRows) {
            totalAllocated += row.getAllocatedAmount();
            totalSpent += row.getActualAmount();
            budgetCategories.add(new BudgetCategoryPoint(
                    row.getCategoryCode(),
                    row.getCategoryName(),
                    row.getAllocatedAmount(),
                    row.getActualAmount()));
        }
        String monthLabel = MONTH_LABEL.format(currentMonth);
        BudgetReport budget = new BudgetReport(
                familyId + ":" + currentMonth,
                monthLabel,
                totalAllocated,
                totalSpent,
                budgetCategories
        );

        return new AnalyticsReportResponse(trends, byCategory, byMember, budget);
    }

    private static String shortMemberName(UUID memberId) {
        String raw = memberId.toString();
        return "TV-" + raw.substring(0, 8);
    }
}