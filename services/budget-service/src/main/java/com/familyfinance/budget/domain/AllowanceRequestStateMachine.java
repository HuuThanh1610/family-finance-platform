package com.familyfinance.budget.domain;

import com.familyfinance.budget.entity.AllowanceRequest;
import com.familyfinance.budget.exception.InvalidAllowanceStateException;

import java.time.Instant;

public final class AllowanceRequestStateMachine {

    private AllowanceRequestStateMachine() {
    }

    public static void transition(AllowanceRequest request, AllowanceRequestStatus target) {
        AllowanceRequestStatus current = AllowanceRequestStatus.fromDbValue(request.getStatus());
        if (!current.canTransitionTo(target)) {
            throw new InvalidAllowanceStateException(
                    "Cannot transition allowance request %s from %s to %s"
                            .formatted(request.getId(), current, target));
        }
        request.setStatus(target.name());
        request.setUpdatedAt(Instant.now());
        if (target == AllowanceRequestStatus.REJECTED
                || target == AllowanceRequestStatus.CANCELLED
                || target == AllowanceRequestStatus.DISBURSED
                || target == AllowanceRequestStatus.APPROVED_PENDING_DISBURSEMENT) {
            request.setDecidedAt(Instant.now());
        }
    }
}