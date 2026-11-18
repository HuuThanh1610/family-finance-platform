package com.familyfinance.expense.domain;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ExpenseStatus {
    DRAFT,
    RECORDED,
    SUBMITTED_FOR_PAYMENT,
    PENDING_WALLET,
    PAID,
    FAILED,
    VOID;

    private static final Map<ExpenseStatus, Set<ExpenseStatus>> TRANSITIONS = Map.of(
            DRAFT, EnumSet.of(RECORDED, SUBMITTED_FOR_PAYMENT, VOID),
            RECORDED, EnumSet.of(SUBMITTED_FOR_PAYMENT, VOID),
            SUBMITTED_FOR_PAYMENT, EnumSet.of(PENDING_WALLET, FAILED, VOID),
            PENDING_WALLET, EnumSet.of(PAID, FAILED),
            PAID, EnumSet.noneOf(ExpenseStatus.class),
            FAILED, EnumSet.of(SUBMITTED_FOR_PAYMENT, VOID),
            VOID, EnumSet.noneOf(ExpenseStatus.class)
    );

    public boolean canTransitionTo(ExpenseStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
