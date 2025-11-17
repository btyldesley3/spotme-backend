package com.spotme.domain.model.exercise;

import java.util.UUID;

public record ExerciseDefinitionId(UUID value) {
    public ExerciseDefinitionId {
        if (value == null) {
            throw new IllegalArgumentException("ExerciseDefinitionId value cannot be null.");
        }
    }

    public static ExerciseDefinitionId generate() {
        return new ExerciseDefinitionId(UUID.randomUUID());
    }

    public static ExerciseDefinitionId fromString(String raw) {
        return new ExerciseDefinitionId(UUID.fromString(raw));
    }
}
