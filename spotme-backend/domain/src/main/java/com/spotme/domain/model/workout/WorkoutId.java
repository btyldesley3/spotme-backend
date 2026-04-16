package com.spotme.domain.model.workout;

import java.util.Objects;
import java.util.UUID;

public class WorkoutId {
    private final UUID value;

    public WorkoutId(UUID value) {
        this.value = Objects.requireNonNull(value, "WorkoutId value cannot be null");
    }

    public static WorkoutId random() {
        return new WorkoutId(UUID.randomUUID());
    }

    public static WorkoutId fromString(String value) {
        return new WorkoutId(UUID.fromString(value));
    }

    public UUID value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkoutId workoutId = (WorkoutId) o;
        return value.equals(workoutId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

