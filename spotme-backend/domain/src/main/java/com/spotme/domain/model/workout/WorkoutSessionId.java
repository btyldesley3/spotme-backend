package com.spotme.domain.model.workout;

import java.util.Objects;
import java.util.UUID;

public record WorkoutSessionId(
        UUID value
) {
    public WorkoutSessionId {
        Objects.requireNonNull(value, "WorkoutSessionId cannot be null");
    }

    public static WorkoutSessionId random() {
        return new WorkoutSessionId(UUID.randomUUID());
    }

    public static WorkoutSessionId fromString(String s) {
        return new WorkoutSessionId(UUID.fromString(s));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
