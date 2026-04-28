create table if not exists workflows.consumed_messages (
    consumed_message_id uuid primary key,
    source varchar(64) not null,
    message_id varchar(128) not null,
    payload_hash varchar(128) not null,
    consumed_at timestamptz not null,
    constraint uk_workflow_consumed_message unique (source, message_id)
);

create index if not exists idx_workflow_consumed_messages_source_time
    on workflows.consumed_messages (source, consumed_at desc);
