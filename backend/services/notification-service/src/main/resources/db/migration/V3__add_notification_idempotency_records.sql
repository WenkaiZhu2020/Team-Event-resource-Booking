CREATE TABLE IF NOT EXISTS notifications.idempotency_records (
    idempotency_record_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    last_processed_message_id UUID NOT NULL,
    last_processed_event_type VARCHAR(120) NOT NULL,
    last_processed_event_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_notification_idempotency_aggregate
    ON notifications.idempotency_records(aggregate_id);
