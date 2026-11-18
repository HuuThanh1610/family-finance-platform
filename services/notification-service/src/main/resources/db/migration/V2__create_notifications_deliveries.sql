CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id   UUID NOT NULL,
    family_id           UUID,
    template_key        VARCHAR(128),
    channel             VARCHAR(32) NOT NULL,
    title               VARCHAR(300) NOT NULL,
    body                TEXT NOT NULL,
    payload             JSONB NOT NULL DEFAULT '{}'::jsonb,
    status              VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('EMAIL', 'PUSH', 'SMS', 'IN_APP')),
    CONSTRAINT chk_notifications_status CHECK (
        status IN ('QUEUED', 'SENT', 'DELIVERED', 'FAILED', 'READ', 'CANCELLED')
    )
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_family_id ON notifications (family_id);
CREATE INDEX idx_notifications_status ON notifications (status);

CREATE TABLE notification_deliveries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id     UUID NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    attempt_no          INT NOT NULL,
    provider            VARCHAR(64),
    provider_message_id VARCHAR(200),
    status              VARCHAR(32) NOT NULL,
    error_message       VARCHAR(500),
    attempted_at        TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_notification_deliveries_attempt UNIQUE (notification_id, attempt_no),
    CONSTRAINT chk_notification_deliveries_status CHECK (
        status IN ('PENDING', 'SENT', 'DELIVERED', 'BOUNCED', 'FAILED')
    )
);

CREATE INDEX idx_notification_deliveries_notification_id ON notification_deliveries (notification_id);