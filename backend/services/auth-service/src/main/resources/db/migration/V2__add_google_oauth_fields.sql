alter table auth.app_users
    add column if not exists display_name varchar(255),
    add column if not exists avatar_url varchar(512),
    add column if not exists email_verified boolean not null default false,
    add column if not exists google_subject varchar(255),
    add column if not exists google_linked_at timestamptz;

create unique index if not exists uq_app_users_google_subject
    on auth.app_users (google_subject)
    where google_subject is not null;
