package com.spotme.domain.model.exercise;

public record Exercise(
        ExerciseId id, String name, Modality modality) extends com.spotme.domain.model.program.exercise.Exercise {
    public enum Modality { BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT}

    public Exercise {
        if (id == null) throw new IllegalArgumentException("ExerciseId cannot be null");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Exercise name cannot be null or blank");
        if (modality == null) throw new IllegalArgumentException("Modality cannot be null");
    }
}

