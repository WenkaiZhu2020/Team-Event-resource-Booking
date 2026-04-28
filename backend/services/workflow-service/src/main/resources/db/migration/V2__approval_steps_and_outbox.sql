create table if not exists workflows.approval_steps (
    step_id uuid primary key,
    approval_id uuid not null references workflows.approval_requests(approval_id) on delete cascade,
    step_number integer not null,
    approver_id uuid not null,
    status varchar(32) not null,
    decision_note varchar(400),
    decided_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_approval_step_number unique (approval_id, step_number)
);

create table if not exists workflows.workflow_outbox (
    message_id uuid primary key,
    aggregate_type varchar(64) not null,
    aggregate_id uuid not null,
    event_type varchar(128) not null,
    payload text not null,
    status varchar(32) not null,
    created_at timestamptz not null,
    published_at timestamptz
);

create index if not exists idx_approval_steps_approval
    on workflows.approval_steps (approval_id, step_number asc);

create index if not exists idx_workflow_outbox_status
    on workflows.workflow_outbox (status, created_at asc);
