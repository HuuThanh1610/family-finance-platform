CREATE TABLE family_audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    actor_user_id       UUID,
    action              VARCHAR(64) NOT NULL,
    entity_type         VARCHAR(64),
    entity_id           UUID,
    metadata            JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at         TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE INDEX idx_family_audit_logs_family_id ON family_audit_logs (family_id);
CREATE INDEX idx_family_audit_logs_occurred_at ON family_audit_logs (occurred_at DESC);

COMMENT ON TABLE family_audit_logs IS 'Audit bất biến; không có updated_at.';