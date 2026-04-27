CREATE SCHEMA IF NOT EXISTS notifications;

CREATE TABLE IF NOT EXISTS notifications.notification_records (
    notification_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    source_event_id UUID NOT NULL,
    source_event_type VARCHAR(100) NOT NULL,
    notification_type VARCHAR(60) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    read_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    failure_reason VARCHAR(400),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notification_delivery UNIQUE (source_event_id, user_id, channel)
);

CREATE TABLE IF NOT EXISTS notifications.processed_events (
    processed_event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_notification_user_created
    ON notifications.notification_records(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_user_status
    ON notifications.notification_records(user_id, status);
CREATE INDEX IF NOT EXISTS idx_processed_events_type
    ON notifications.processed_events(event_type, processed_at DESC);
