CREATE TABLE notification_preferences (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id     UUID NOT NULL,
    channel             VARCHAR(32) NOT NULL,
    category            VARCHAR(64) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    CONSTRAINT uq_notification_preferences_user_channel_cat UNIQUE (user_profile_id, channel, category),
    CONSTRAINT chk_notification_preferences_channel CHECK (
        channel IN ('EMAIL', 'PUSH', 'SMS', 'IN_APP')
    )
);

CREATE INDEX idx_notification_preferences_user ON notification_preferences (user_profile_id);