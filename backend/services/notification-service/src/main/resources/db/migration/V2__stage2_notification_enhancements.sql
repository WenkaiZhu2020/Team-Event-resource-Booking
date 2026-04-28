CREATE TABLE IF NOT EXISTS notifications.notification_templates (
    id UUID PRIMARY KEY,
    template_code VARCHAR(80) NOT NULL,
    channel VARCHAR(24) NOT NULL,
    title_template VARCHAR(300) NOT NULL,
    body_template TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_template_code_channel
    ON notifications.notification_templates(template_code, channel);

CREATE TABLE IF NOT EXISTS notifications.notification_preferences (
    user_id UUID PRIMARY KEY,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_lead_minutes INTEGER NOT NULL DEFAULT 60,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS notifications.notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    channel VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL,
    title VARCHAR(300) NOT NULL,
    body TEXT NOT NULL,
    template_code VARCHAR(80),
    source VARCHAR(60) NOT NULL,
    source_event_type VARCHAR(120),
    reference_type VARCHAR(64),
    reference_id UUID,
    idempotency_key VARCHAR(180) NOT NULL,
    metadata_json TEXT,
    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notifications_idempotency ON notifications.notifications(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications.notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_status_retry ON notifications.notifications(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled ON notifications.notifications(status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_notifications_reference ON notifications.notifications(reference_type, reference_id);

INSERT INTO notifications.notifications (
    id, user_id, type, channel, status, title, body, template_code, source, source_event_type,
    reference_type, reference_id, idempotency_key, metadata_json, scheduled_at, sent_at, read_at,
    next_retry_at, retry_count, max_retries, last_error, created_at, updated_at, version
)
SELECT notification_id,
       user_id,
       CASE notification_type
           WHEN 'BOOKING_CREATED' THEN 'BOOKING_CONFIRMED'
           ELSE notification_type
       END,
       channel,
       status,
       subject,
       body,
       NULL,
       'system',
       source_event_type,
       NULL,
       source_event_id,
       CONCAT(source_event_id::text, ':', user_id::text, ':', channel),
       NULL,
       NULL,
       sent_at,
       read_at,
       NULL,
       0,
       3,
       failure_reason,
       created_at,
       updated_at,
       0
FROM notifications.notification_records
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS notifications.delivery_attempts (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications.notifications(id) ON DELETE CASCADE,
    channel VARCHAR(24) NOT NULL,
    attempt_no INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider_message_id VARCHAR(120),
    error_message VARCHAR(500),
    attempted_at TIMESTAMPTZ NOT NULL,
    duration_ms BIGINT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_delivery_attempts_notification ON notifications.delivery_attempts(notification_id, attempt_no DESC);

CREATE TABLE IF NOT EXISTS notifications.consumed_messages (
    id UUID PRIMARY KEY,
    source VARCHAR(80) NOT NULL,
    message_id VARCHAR(150) NOT NULL,
    payload_hash VARCHAR(128),
    consumed_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_consumed_source_message
    ON notifications.consumed_messages(source, message_id);

INSERT INTO notifications.consumed_messages (id, source, message_id, payload_hash, consumed_at)
SELECT processed_event_id, event_type, processed_event_id::text, NULL, processed_at
FROM notifications.processed_events
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS notifications.notification_outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(24) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notification_outbox_status ON notifications.notification_outbox(status, created_at);

INSERT INTO notifications.notification_templates (
    id, template_code, channel, title_template, body_template, active, created_at, updated_at, version
) VALUES
('10000000-0000-0000-0000-000000000001', 'BOOKING_CONFIRMED', 'IN_APP', 'Booking Confirmed', 'Your booking {{bookingId}} for resource {{resourceId}} is confirmed from {{startAt}} to {{endAt}}.', TRUE, now(), now(), 0),
('10000000-0000-0000-0000-000000000002', 'BOOKING_CONFIRMED', 'EMAIL', 'Booking Confirmed', 'Booking {{bookingId}} has been confirmed. Time: {{startAt}} - {{endAt}}.', TRUE, now(), now(), 0),
('10000000-0000-0000-0000-000000000003', 'BOOKING_REJECTED', 'IN_APP', 'Booking Rejected', 'Your booking {{bookingId}} was rejected. Reason: {{reason}}', TRUE, now(), now(), 0),
('10000000-0000-0000-0000-000000000004', 'BOOKING_REJECTED', 'EMAIL', 'Booking Rejected', 'Booking {{bookingId}} was rejected. Reason: {{reason}}.', TRUE, now(), now(), 0),
('10000000-0000-0000-0000-000000000005', 'BOOKING_APPROVED', 'IN_APP', 'Approval Completed', 'Your booking {{bookingId}} was approved.', TRUE, now(), now(), 0),
('10000000-0000-0000-0000-000000000006', 'WAITLIST_PROMOTED', 'IN_APP', 'Waitlist Promoted', 'Good news. Booking {{bookingId}} has been promoted from waitlist.', TRUE, now(), now(), 0),
('10000000-0000-0000-0000-000000000007', 'EVENT_REMINDER', 'IN_APP', 'Booking Reminder', 'Reminder: booking {{bookingId}} starts at {{startAt}}.', TRUE, now(), now(), 0),
('10000000-0000-0000-0000-000000000008', 'BOOKING_CANCELLED', 'IN_APP', 'Booking Cancelled', 'Booking {{bookingId}} was cancelled.', TRUE, now(), now(), 0)
ON CONFLICT (template_code, channel) DO NOTHING;
