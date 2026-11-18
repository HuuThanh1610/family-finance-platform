package com.familyfinance.budget.service;

import com.familyfinance.budget.entity.Budget;
import com.familyfinance.budget.entity.BudgetCategory;
import com.familyfinance.budget.repository.BudgetCategoryRepository;
import com.familyfinance.budget.repository.BudgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock BudgetRepository budgetRepository;
    @Mock BudgetCategoryRepository categoryRepository;

    BudgetService service;

    UUID budgetId = UUID.fromString("12000000-0000-4000-8000-000000000001");

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgetRepository, categoryRepository);
    }

    @Test
    void computesRemainingAndOverBudgetFromCategories() {
        Budget budget = new Budget();
        budget.setId(budgetId);
        budget.setFamilyId(UUID.randomUUID());
        budget.setName("Nov");
        budget.setPeriodStart(LocalDate.of(2026, 11, 1));
        budget.setPeriodEnd(LocalDate.of(2026, 11, 30));
        budget.setTotalLimit(15_000_000L);
        budget.setCurrencyCode("VND");
        budget.setStatus("ACTIVE");
        budget.setCreatedAt(Instant.now());
        budget.setUpdatedAt(Instant.now());

        BudgetCategory food = category("FOOD", 6_000_000L, 7_200_000L);
        BudgetCategory edu = category("EDUCATION", 3_000_000L, 0L);

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(categoryRepository.findByBudgetIdOrderByCategoryCodeAsc(budgetId)).thenReturn(List.of(food, edu));

        var response = service.findById(budgetId);

        assertThat(response.totalSpent()).isEqualTo(7_200_000L);
        assertThat(response.remainingAmount()).isEqualTo(7_800_000L);
        assertThat(response.overBudget()).isFalse();
        assertThat(response.categories()).hasSize(2);
        assertThat(response.categories().getFirst().overBudget()).isTrue();
    }

    private static BudgetCategory category(String code, long allocated, long spent) {
        BudgetCategory c = new BudgetCategory();
        c.setId(UUID.randomUUID());
        c.setCategoryCode(code);
        c.setDisplayName(code);
        c.setAllocatedAmount(allocated);
        c.setSpentAmount(spent);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }
}