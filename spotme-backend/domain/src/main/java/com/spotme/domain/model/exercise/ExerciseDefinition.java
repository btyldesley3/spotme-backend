package com.spotme.domain.model.exercise;

import java.util.Set;

public class ExerciseDefinition {
    private final ExerciseDefinitionId id;
    private final String name;
    private final Set<MuscleGroup> primaryMuscles;
    private final Set<MuscleGroup> secondaryMuscles;
    private final MovementPattern movementPattern;
    private final String equipment;
    private final String videoUrl;

    public ExerciseDefinition(ExerciseDefinitionId id,
                              String name,
                              Set<MuscleGroup> primaryMuscles,
                              Set<MuscleGroup> secondaryMuscles,
                              MovementPattern movementPattern,
                              String equipment,
                              String videoUrl) {
        this.id = id;
        this.name = name;
        this.primaryMuscles = primaryMuscles;
        this.secondaryMuscles = secondaryMuscles;
        this.movementPattern = movementPattern;
        this.equipment = equipment;
        this.videoUrl = videoUrl;
    }

    public ExerciseDefinitionId getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
