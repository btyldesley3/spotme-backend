package com.spotme.domain.model.plan;

import com.spotme.domain.model.exercise.ExerciseId;

public record SetPrescription(
        ExerciseId exerciseId,
        int order,
        int prescribedReps,
        double prescribedWeightKg,
        boolean backoff
) {
    public SetPrescription {
        if (exerciseId == null) throw new IllegalArgumentException("ExerciseId cannot be null");
        if (order < 1) throw new IllegalArgumentException("Order must be at least 1");
        if (prescribedReps < 1) throw new IllegalArgumentException("Prescribed reps cannot be negative or zero");
        if (prescribedWeightKg < 0) throw new IllegalArgumentException("Prescribed weight cannot be negative or zero");
    }
}
