CREATE TABLE user_consents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id   UUID NOT NULL REFERENCES user_profiles (id) ON DELETE CASCADE,
    consent_type      VARCHAR(64) NOT NULL,
    policy_version    VARCHAR(32) NOT NULL,
    granted           BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at        TIMESTAMPTZ,
    revoked_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_user_consents_profile_type_version UNIQUE (user_profile_id, consent_type, policy_version),
    CONSTRAINT chk_user_consents_revoked CHECK (
        revoked_at IS NULL OR (granted = FALSE AND granted_at IS NOT NULL)
    )
);

CREATE INDEX idx_user_consents_profile_id ON user_consents (user_profile_id);