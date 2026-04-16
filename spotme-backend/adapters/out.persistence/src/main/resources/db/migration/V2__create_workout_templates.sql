-- Workout templates table (immutable blueprints for training sessions)
create table if not exists workouts (
    id uuid primary key,
    block_id uuid not null,
    week_number integer not null,
    session_number integer not null,
    version integer not null default 1,
    notes varchar(500),
    set_presets_json text not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_workouts_block_id on workouts(block_id);
create index if not exists idx_workouts_block_week_session on workouts(block_id, week_number, session_number);

