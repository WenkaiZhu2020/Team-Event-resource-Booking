alter table workflows.approval_requests
    add column if not exists approval_scope varchar(32) not null default 'ASSIGNED_USER';
