CREATE TABLE family_roles (
    code            VARCHAR(32) PRIMARY KEY,
    display_name    VARCHAR(100) NOT NULL,
    description     TEXT,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC')
);

CREATE TABLE family_permissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_code       VARCHAR(32) NOT NULL REFERENCES family_roles (code) ON DELETE CASCADE,
    permission_key  VARCHAR(128) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_family_permissions_role_key UNIQUE (role_code, permission_key)
);

CREATE INDEX idx_family_permissions_role_code ON family_permissions (role_code);

INSERT INTO family_roles (code, display_name, sort_order) VALUES
    ('OWNER', 'Chủ hộ', 10),
    ('PARENT', 'Phụ huynh', 20),
    ('ADULT_MEMBER', 'Thành viên trưởng thành', 30),
    ('CHILD', 'Trẻ em', 40),
    ('VIEWER', 'Chỉ xem', 50);

INSERT INTO family_permissions (role_code, permission_key) VALUES
    ('OWNER', 'family.manage'),
    ('OWNER', 'wallet.transfer'),
    ('OWNER', 'budget.approve'),
    ('PARENT', 'wallet.transfer'),
    ('PARENT', 'budget.approve'),
    ('PARENT', 'expense.create'),
    ('ADULT_MEMBER', 'expense.create'),
    ('CHILD', 'allowance.request'),
    ('VIEWER', 'family.read');