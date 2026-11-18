package com.familyfinance.expense.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseStatusTest {

    @Test
    void allowsWalletFundedHappyPath() {
        assertTrue(ExpenseStatus.RECORDED.canTransitionTo(ExpenseStatus.SUBMITTED_FOR_PAYMENT));
        assertTrue(ExpenseStatus.SUBMITTED_FOR_PAYMENT.canTransitionTo(ExpenseStatus.PENDING_WALLET));
        assertTrue(ExpenseStatus.PENDING_WALLET.canTransitionTo(ExpenseStatus.PAID));
    }

    @Test
    void rejectsPaidToDraft() {
        assertFalse(ExpenseStatus.PAID.canTransitionTo(ExpenseStatus.DRAFT));
    }
}
