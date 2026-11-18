CREATE TABLE families (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(120) NOT NULL,
    description     TEXT,
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_families_slug UNIQUE (slug),
    CONSTRAINT chk_families_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_families_status ON families (status);