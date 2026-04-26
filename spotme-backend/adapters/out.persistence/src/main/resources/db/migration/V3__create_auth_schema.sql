-- Alpha invite codes
create table if not exists alpha_invite_codes (
    id           uuid primary key,
    code_hash    varchar(64) not null unique,
    active       boolean not null default true,
    max_uses     integer not null default 1,
    used_count   integer not null default 0,
    expires_at   timestamptz,
    created_at   timestamptz not null default now()
);

-- Alpha email allowlist
create table if not exists alpha_email_allowlist (
    id         uuid primary key,
    email      varchar(320) not null unique,
    active     boolean not null default true,
    notes      text,
    created_at timestamptz not null default now()
);

-- User credentials (separated from users to keep domain aggregate clean)
create table if not exists user_credentials (
    user_id           uuid primary key references users(id) on delete cascade,
    email             varchar(320) not null unique,
    password_hash     varchar(255) not null,
    alpha_eligible    boolean not null default false,
    alpha_access_path varchar(50),
    created_at        timestamptz not null default now()
);

create index if not exists idx_user_credentials_email on user_credentials(email);

-- Refresh tokens (hashed; one-time use via validate-and-consume)
create table if not exists refresh_tokens (
    id          uuid primary key,
    user_id     uuid not null references users(id) on delete cascade,
    token_hash  varchar(64) not null unique,
    expires_at  timestamptz not null,
    revoked     boolean not null default false,
    created_at  timestamptz not null default now()
);

create index if not exists idx_refresh_tokens_user_id  on refresh_tokens(user_id);
create index if not exists idx_refresh_tokens_hash     on refresh_tokens(token_hash);


