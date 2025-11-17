package com.spotme.domain.rules;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.plan.SetPrescription;

import java.util.List;

public final class ProgressionEngine {
    public Prescription nextFor(ExerciseId exerciseId, ProgressionInput input, ProgressionPolicy policy) {
        // Simplified MVP:
        // - If DOMS severe -> mirror last session (same reps/weight)
        // - Else micro-load the weight, keep reps
        double nextWeight = input.lastTopSetWeightKg();
        if (input.doms().value() >= policy.severeDomsThreshold()) {
            // mirror
        } else {
            nextWeight = roundToPlate(nextWeight + policy.microLoadKg());
        }
        var set = new SetPrescription(exerciseId, 1, input.lastTopSetReps(), nextWeight, false);
        return new Prescription(List.of(set));
    }

    private double roundToPlate(double kg) {
        // Round to nearest 0.25 kg (change if you want lb plates)
        return Math.round(kg * 4.0) / 4.0;
    }
}
