package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;

public record ExerciseSessionSummary(
        ExerciseId exerciseId,
        int totalSets,
        int totalReps,
        double totalVolumeKg,
        SetEntry topSet
) {
}

