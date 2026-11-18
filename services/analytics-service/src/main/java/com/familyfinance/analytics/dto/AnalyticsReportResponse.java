package com.familyfinance.analytics.dto;

import java.util.List;

public record AnalyticsReportResponse(
        List<MonthlyTrendPoint> monthlyTrends,
        List<CategorySpendPoint> spendingByCategory,
        List<MemberSpendPoint> spendingByMember,
        BudgetReport budget
) {
    public record MonthlyTrendPoint(String month, long income, long expense) {
    }

    public record CategorySpendPoint(String category, long amount) {
    }

    public record MemberSpendPoint(String memberName, long amount) {
    }

    public record BudgetReport(
            String id,
            String month,
            long totalAllocated,
            long totalSpent,
            List<BudgetCategoryPoint> categories
    ) {
    }

    public record BudgetCategoryPoint(String id, String name, long allocated, long spent) {
    }
}