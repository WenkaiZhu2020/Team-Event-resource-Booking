alter table events.events
    add column if not exists checked_in_count integer not null default 0;

alter table events.event_registrations
    add column if not exists checked_in_at timestamptz,
    add column if not exists checked_in_by uuid;
