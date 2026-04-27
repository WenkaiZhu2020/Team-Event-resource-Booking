create schema if not exists workflows;

create table if not exists workflows.approval_requests (
    approval_id uuid primary key,
    target_type varchar(32) not null,
    target_id uuid not null,
    approval_type varchar(64) not null,
    requester_id uuid not null,
    approver_id uuid not null,
    target_owner_id uuid,
    resource_id uuid,
    title varchar(200) not null,
    summary varchar(1000),
    current_step integer not null,
    total_steps integer not null,
    status varchar(32) not null,
    submitted_at timestamptz not null,
    decided_at timestamptz,
    decision_note varchar(400),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint uk_workflow_target unique (target_type, target_id)
);

create table if not exists workflows.approval_decision_history (
    decision_id uuid primary key,
    approval_id uuid not null references workflows.approval_requests(approval_id) on delete cascade,
    action varchar(32) not null,
    actor_id uuid,
    note varchar(400),
    acted_at timestamptz not null,
    created_at timestamptz not null
);

create index if not exists idx_approval_requests_approver_status
    on workflows.approval_requests (approver_id, status, created_at desc);

create index if not exists idx_approval_requests_requester_created
    on workflows.approval_requests (requester_id, created_at desc);

create index if not exists idx_approval_history_approval
    on workflows.approval_decision_history (approval_id, acted_at asc);
