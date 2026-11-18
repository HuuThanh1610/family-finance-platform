CREATE TABLE family_members (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    user_profile_id     UUID NOT NULL,
    role_code           VARCHAR(32) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    nickname            VARCHAR(100),
    joined_at           TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_family_members_family_user UNIQUE (family_id, user_profile_id),
    CONSTRAINT chk_family_members_role CHECK (
        role_code IN ('OWNER', 'PARENT', 'ADULT_MEMBER', 'CHILD', 'VIEWER')
    ),
    CONSTRAINT chk_family_members_status CHECK (status IN ('ACTIVE', 'LEFT', 'REMOVED'))
);

CREATE INDEX idx_family_members_family_id ON family_members (family_id);
CREATE INDEX idx_family_members_user_profile_id ON family_members (user_profile_id);

COMMENT ON COLUMN family_members.user_profile_id IS 'UUID tham chiếu logic tới profile_db.user_profiles (không FK xuyên DB).';