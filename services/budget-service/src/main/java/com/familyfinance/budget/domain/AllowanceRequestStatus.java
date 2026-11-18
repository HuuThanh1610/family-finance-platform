package com.familyfinance.budget.domain;

import java.util.EnumSet;
import java.util.Set;

public enum AllowanceRequestStatus {
    PENDING,
    APPROVED_PENDING_DISBURSEMENT,
    DISBURSED,
    REJECTED,
    DISBURSEMENT_FAILED,
    CANCELLED,
    /** Legacy seed / pre-V6 rows */
    APPROVED;

    private static final Set<AllowanceRequestStatus> PENDING_CANCELLABLE =
            EnumSet.of(PENDING, APPROVED_PENDING_DISBURSEMENT, APPROVED);

    public static AllowanceRequestStatus fromDbValue(String value) {
        return valueOf(value);
    }

    public boolean canTransitionTo(AllowanceRequestStatus target) {
        return switch (this) {
            case PENDING -> target == APPROVED_PENDING_DISBURSEMENT
                    || target == REJECTED
                    || target == CANCELLED;
            case APPROVED_PENDING_DISBURSEMENT -> target == DISBURSED
                    || target == DISBURSEMENT_FAILED
                    || target == CANCELLED;
            case APPROVED -> target == DISBURSED
                    || target == DISBURSEMENT_FAILED
                    || target == CANCELLED;
            case DISBURSEMENT_FAILED -> target == APPROVED_PENDING_DISBURSEMENT
                    || target == DISBURSED;
            default -> false;
        };
    }

    public boolean isCancellableByRequester() {
        return PENDING_CANCELLABLE.contains(this);
    }
}