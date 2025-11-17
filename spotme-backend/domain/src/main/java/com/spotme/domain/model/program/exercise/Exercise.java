package com.spotme.domain.model.program.exercise;

import com.spotme.domain.model.exercise.ExerciseDefinitionId;
import com.spotme.domain.model.exercise.ExerciseId;

import java.util.List;
import java.util.Objects;

public class Exercise {

    private final ExerciseId id;
    private final ExerciseDefinitionId definitionId;
    private final List<ExerciseSet> sets;

    public Exercise(ExerciseId id, ExerciseDefinitionId definitionId, List<ExerciseSet> sets) {
        this.id = Objects.requireNonNull(id);
        this.definitionId = Objects.requireNonNull(definitionId);
        this.sets = List.copyOf(sets);
    }

    public ExerciseId getId() {
        return id;
    }

    public ExerciseDefinitionId getDefinitionId() {
        return definitionId;
    }

    public List<ExerciseSet> getSets() {
        return sets;
    }
}
