CREATE SCHEMA IF NOT EXISTS resources;

CREATE TABLE IF NOT EXISTS resources.resources (
    resource_id UUID PRIMARY KEY,
    manager_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    type VARCHAR(32) NOT NULL,
    location VARCHAR(180) NOT NULL,
    capacity INTEGER,
    status VARCHAR(32) NOT NULL,
    approval_mode VARCHAR(32) NOT NULL,
    allow_waitlist BOOLEAN NOT NULL,
    max_booking_duration_minutes INTEGER NOT NULL,
    advance_booking_window_days INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT
);

CREATE TABLE IF NOT EXISTS resources.availability_rules (
    availability_rule_id UUID PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES resources.resources(resource_id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    available BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS resources.maintenance_slots (
    maintenance_slot_id UUID PRIMARY KEY,
    resource_id UUID NOT NULL REFERENCES resources.resources(resource_id) ON DELETE CASCADE,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(240) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_resources_manager_status ON resources.resources(manager_id, status);
CREATE INDEX IF NOT EXISTS idx_resources_type_status ON resources.resources(type, status);
CREATE INDEX IF NOT EXISTS idx_resources_approval_status ON resources.resources(approval_mode, status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_availability_rules_window
    ON resources.availability_rules(resource_id, day_of_week, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_maintenance_resource_starts
    ON resources.maintenance_slots(resource_id, starts_at);
