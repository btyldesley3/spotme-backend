package com.spotme.domain.model.program.exercise;

import java.util.UUID;

public record ExerciseId(UUID value) {
    public ExerciseId {
        if (value == null) throw new IllegalArgumentException("ExerciseId cannot be null");
    }

    public static ExerciseId random() {
        return new ExerciseId(UUID.randomUUID());
    }

    public static ExerciseId fromString(String s) {
        return new ExerciseId(UUID.fromString(s));
    }

}
