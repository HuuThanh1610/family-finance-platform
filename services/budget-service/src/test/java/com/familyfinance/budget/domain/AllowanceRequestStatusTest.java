package com.familyfinance.budget.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AllowanceRequestStatusTest {

    @Test
    void pendingCanApproveRejectOrCancel() {
        assertThat(AllowanceRequestStatus.PENDING.canTransitionTo(AllowanceRequestStatus.APPROVED_PENDING_DISBURSEMENT))
                .isTrue();
        assertThat(AllowanceRequestStatus.PENDING.canTransitionTo(AllowanceRequestStatus.REJECTED)).isTrue();
        assertThat(AllowanceRequestStatus.PENDING.canTransitionTo(AllowanceRequestStatus.CANCELLED)).isTrue();
        assertThat(AllowanceRequestStatus.PENDING.canTransitionTo(AllowanceRequestStatus.DISBURSED)).isFalse();
    }

    @Test
    void approvedPendingDisbursementWaitsForWallet() {
        assertThat(AllowanceRequestStatus.APPROVED_PENDING_DISBURSEMENT.canTransitionTo(AllowanceRequestStatus.DISBURSED))
                .isTrue();
        assertThat(AllowanceRequestStatus.APPROVED_PENDING_DISBURSEMENT.canTransitionTo(AllowanceRequestStatus.DISBURSEMENT_FAILED))
                .isTrue();
        assertThat(AllowanceRequestStatus.APPROVED.canTransitionTo(AllowanceRequestStatus.DISBURSED)).isTrue();
    }
}