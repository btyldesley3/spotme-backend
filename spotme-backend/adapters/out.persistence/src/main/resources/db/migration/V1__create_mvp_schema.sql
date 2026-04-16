create table if not exists users (
    id uuid primary key,
    experience_level varchar(50) not null,
    training_goal varchar(50) not null,
    baseline_sleep_hours integer not null,
    stress_sensitivity integer not null,
    active_program_id uuid
);

create table if not exists workout_sessions (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    started_at timestamptz not null,
    finished_at timestamptz,
    doms integer,
    sleep_quality integer,
    min_total_sets integer,
    min_distinct_exercises integer,
    min_sets_per_exercise integer,
    require_recovery_feedback_for_progression boolean
);

create index if not exists idx_workout_sessions_user_id on workout_sessions(user_id);
create index if not exists idx_workout_sessions_started_at on workout_sessions(started_at);
create index if not exists idx_workout_sessions_finished_at on workout_sessions(finished_at);

create table if not exists workout_sets (
    id bigserial primary key,
    session_id uuid not null references workout_sessions(id) on delete cascade,
    exercise_id uuid not null,
    set_number integer not null,
    reps integer not null,
    weight_kg double precision not null,
    rpe double precision not null,
    notes text,
    constraint uq_workout_sets_session_exercise_set unique(session_id, exercise_id, set_number)
);

create index if not exists idx_workout_sets_session_id on workout_sets(session_id);
create index if not exists idx_workout_sets_exercise_id on workout_sets(exercise_id);

create table if not exists prescriptions (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    created_at timestamptz not null,
    payload_json text not null
);

create index if not exists idx_prescriptions_user_id on prescriptions(user_id);
create index if not exists idx_prescriptions_created_at on prescriptions(created_at);

