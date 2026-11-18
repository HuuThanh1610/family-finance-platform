-- Extend payment order status CHECK for state machine (keep legacy values for V99 seed).
ALTER TABLE payment_orders DROP CONSTRAINT chk_payment_orders_status;

ALTER TABLE payment_orders ADD CONSTRAINT chk_payment_orders_status CHECK (
    status IN (
        'CREATED',
        'PENDING_PROVIDER',
        'PROVIDER_PAID',
        'CREDIT_PENDING',
        'COMPLETED',
        'FAILED',
        'EXPIRED',
        'CANCELLED',
        'PENDING',
        'AUTHORIZED',
        'CAPTURED',
        'REFUNDED'
    )
);