CREATE TABLE user_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_sub    VARCHAR(128) NOT NULL,
    email           VARCHAR(320) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    phone           VARCHAR(32),
    avatar_url      VARCHAR(2048),
    locale          VARCHAR(16) NOT NULL DEFAULT 'vi-VN',
    timezone        VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    status          VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_user_profiles_keycloak_sub UNIQUE (keycloak_sub),
    CONSTRAINT uq_user_profiles_email UNIQUE (email),
    CONSTRAINT chk_user_profiles_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_user_profiles_status ON user_profiles (status);

COMMENT ON TABLE user_profiles IS 'Hồ sơ người dùng; keycloak_sub là định danh IdP.';