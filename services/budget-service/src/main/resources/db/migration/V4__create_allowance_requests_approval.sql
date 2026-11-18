CREATE TABLE allowance_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    requester_member_id UUID NOT NULL,
    amount              BIGINT NOT NULL,
    reason              VARCHAR(500) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    decided_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_allowance_requests_amount CHECK (amount > 0),
    CONSTRAINT chk_allowance_requests_status CHECK (
        status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
    )
);

CREATE INDEX idx_allowance_requests_family_id ON allowance_requests (family_id);
CREATE INDEX idx_allowance_requests_status ON allowance_requests (status);

CREATE TABLE approval_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    allowance_request_id UUID NOT NULL REFERENCES allowance_requests (id) ON DELETE CASCADE,
    approver_member_id  UUID NOT NULL,
    decision            VARCHAR(32) NOT NULL,
    comment             VARCHAR(500),
    decided_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_approval_history_decision CHECK (decision IN ('APPROVED', 'REJECTED'))
);

CREATE INDEX idx_approval_history_request_id ON approval_history (allowance_request_id);