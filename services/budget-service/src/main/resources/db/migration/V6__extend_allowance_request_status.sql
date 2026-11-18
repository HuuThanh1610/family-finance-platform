ALTER TABLE allowance_requests
    DROP CONSTRAINT chk_allowance_requests_status;

ALTER TABLE allowance_requests
    ADD CONSTRAINT chk_allowance_requests_status CHECK (
        status IN (
            'PENDING',
            'APPROVED_PENDING_DISBURSEMENT',
            'DISBURSED',
            'REJECTED',
            'DISBURSEMENT_FAILED',
            'CANCELLED',
            'APPROVED'
        )
    );