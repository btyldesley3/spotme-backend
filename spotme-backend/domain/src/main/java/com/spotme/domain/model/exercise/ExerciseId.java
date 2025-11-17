package com.spotme.domain.model.exercise;

import com.spotme.domain.model.user.UserId;

import java.util.Objects;
import java.util.UUID;

public record ExerciseId(
        UUID value) {
    public ExerciseId {
        Objects.requireNonNull(value, "ExerciseId cannot be null");
    }

    public static ExerciseId random() {
        return new ExerciseId(UUID.randomUUID());
    }

    public static ExerciseId fromString(String s) {
        return new ExerciseId(UUID.fromString(s));
    }

    @Override public String toString() {
        return value.toString();
    }
}
