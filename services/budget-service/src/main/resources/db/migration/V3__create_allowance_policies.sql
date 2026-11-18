CREATE TABLE allowance_policies (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    member_id           UUID NOT NULL,
    amount              BIGINT NOT NULL,
    cadence             VARCHAR(32) NOT NULL,
    day_of_week         SMALLINT,
    day_of_month        SMALLINT,
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_allowance_policies_amount CHECK (amount > 0),
    CONSTRAINT chk_allowance_policies_cadence CHECK (
        cadence IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'ONE_TIME')
    ),
    CONSTRAINT chk_allowance_policies_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED'))
);

CREATE INDEX idx_allowance_policies_family_id ON allowance_policies (family_id);
CREATE INDEX idx_allowance_policies_member_id ON allowance_policies (member_id);