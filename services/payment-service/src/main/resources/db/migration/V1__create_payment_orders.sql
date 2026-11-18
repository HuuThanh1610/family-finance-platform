CREATE TABLE payment_orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id           UUID NOT NULL,
    payer_member_id     UUID,
    amount              BIGINT NOT NULL,
    currency_code       CHAR(3) NOT NULL DEFAULT 'VND',
    purpose             VARCHAR(64) NOT NULL,
    external_reference  VARCHAR(128),
    status              VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_payment_orders_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_orders_currency CHECK (currency_code = 'VND'),
    CONSTRAINT chk_payment_orders_status CHECK (
        status IN ('CREATED', 'PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'CANCELLED', 'REFUNDED')
    )
);

CREATE INDEX idx_payment_orders_family_id ON payment_orders (family_id);
CREATE INDEX idx_payment_orders_status ON payment_orders (status);