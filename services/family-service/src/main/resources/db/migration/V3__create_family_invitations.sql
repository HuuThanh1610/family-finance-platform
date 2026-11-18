CREATE TABLE family_invitations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL REFERENCES families (id) ON DELETE CASCADE,
    inviter_member_id   UUID,
    invitee_email       VARCHAR(320) NOT NULL,
    role_code           VARCHAR(32) NOT NULL DEFAULT 'ADULT_MEMBER',
    invite_token        VARCHAR(128) NOT NULL,
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at          TIMESTAMPTZ NOT NULL,
    accepted_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_family_invitations_token UNIQUE (invite_token),
    CONSTRAINT chk_family_invitations_role CHECK (
        role_code IN ('OWNER', 'PARENT', 'ADULT_MEMBER', 'CHILD', 'VIEWER')
    ),
    CONSTRAINT chk_family_invitations_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'REVOKED')
    )
);

CREATE INDEX idx_family_invitations_family_id ON family_invitations (family_id);
CREATE INDEX idx_family_invitations_invitee_email ON family_invitations (invitee_email);