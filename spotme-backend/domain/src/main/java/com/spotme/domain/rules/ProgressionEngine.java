package com.spotme.domain.rules;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.plan.SetPrescription;
import com.spotme.domain.rules.RecoveryAssessment.LoadAdjustmentSignal;

import java.util.List;

/**
 * Enhanced progression engine that combines DOMS, RPE, and sleep quality
 * to make intelligent training load recommendations.
 * Key principles:
 * - DOMS is the primary constraint (severe DOMS almost always reduces/mirrors)
 * - RPE informs readiness (high RPE = needs recovery)
 * - Sleep quality amplifies or dampens recovery capacity
 * - Decisions are weighted, not simple thresholds
 */
public final class ProgressionEngine {

    public Prescription nextFor(ExerciseId exerciseId, ProgressionInput input, ProgressionPolicy policy) {
        // Assess overall recovery using all three signals
        RecoveryAssessment recovery = new RecoveryAssessment(
                input.doms(),
                input.lastTopSetRpe(),
                input.sleepQuality(),
                policy
        );

        // Determine load adjustment based on recovery signal
        double nextWeight = computeNextWeight(input, recovery, policy);
        int nextReps = computeNextReps(input, recovery, policy);

        var set = new SetPrescription(exerciseId, 1, nextReps, nextWeight, false);
        return new Prescription(List.of(set));
    }

    /**
     * Computes the next weight based on recovery assessment.
     * Ensures DOMS-driven constraints are respected.
     */
    private double computeNextWeight(ProgressionInput input, RecoveryAssessment recovery, ProgressionPolicy policy) {
        double currentWeight = input.lastTopSetWeightKg();

        return switch (recovery.getSignal()) {
            case REDUCE_LOAD -> reduceWeight(currentWeight, policy);
            case MIRROR_SESSION -> currentWeight; // Same weight as last session
            case STEADY_PROGRESSION -> mirrorWithRepsGain(currentWeight, recovery, policy);
            case INCREASE_LOAD -> increaseWeight(currentWeight, policy);
        };
    }

    /**
     * Computes the next rep target. Allows rep progression before weight increases.
     */
    private int computeNextReps(ProgressionInput input, RecoveryAssessment recovery, ProgressionPolicy policy) {
        // For REDUCE/MIRROR: keep reps the same to allow load adjustment
        if (recovery.getSignal() == LoadAdjustmentSignal.REDUCE_LOAD ||
            recovery.getSignal() == LoadAdjustmentSignal.MIRROR_SESSION) {
            return input.lastTopSetReps();
        }

        // For STEADY/INCREASE: try to add a rep if not at max range
        // This allows volume increase before weight jumps
        if (input.lastTopSetRpe().value() >= policy.optimalRpeMin() &&
            input.lastTopSetRpe().value() <= policy.optimalRpeMax()) {
            return input.lastTopSetReps() + 1;
        }

        return input.lastTopSetReps();
    }

    /**
     * Reduces weight by at least minLoadReductionPct when recovery is poor.
     * Applies when severe DOMS or very high RPE + poor recovery.
     */
    private double reduceWeight(double currentWeight, ProgressionPolicy policy) {
        double reductionPct = policy.minLoadReductionPct();
        double reducedWeight = currentWeight * (1.0 - reductionPct / 100.0);
        return roundToPlate(reducedWeight);
    }

    /**
     * Maintains weight but allows rep progression.
     * Useful during moderate recovery where volume can increase without intensity.
     */
    private double mirrorWithRepsGain(double currentWeight, RecoveryAssessment recovery, ProgressionPolicy policy) {
        // If sleep is poor, keep weight; if sleep is good, allow small increase
        if (recovery.getSleep().value() <= policy.poorSleepThreshold()) {
            return currentWeight;
        }

        // With good sleep and mild DOMS, consider micro-increment
        if (recovery.getDoms().value() <= 2) {
            return roundToPlate(currentWeight + (policy.microLoadKg() * 0.5));
        }

        return currentWeight;
    }

    /**
     * Increases weight by microload increment when recovery is excellent.
     * Only triggered by INCREASE_LOAD signal (low RPE, minimal DOMS, good sleep).
     */
    private double increaseWeight(double currentWeight, ProgressionPolicy policy) {
        return roundToPlate(currentWeight + policy.microLoadKg());
    }

    /**
     * Rounds weight to nearest 0.25 kg (quarter plate) for safe gym usage.
     * Can be adjusted per user preference (0.5 lb increments, etc.).
     */
    private double roundToPlate(double kg) {
        return Math.round(kg * 4.0) / 4.0;
    }
}
