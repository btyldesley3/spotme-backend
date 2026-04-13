package com.spotme.domain.rules;

import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;

/**
 * Aggregates DOMS, RPE, and sleep into a holistic recovery assessment.
 * Encapsulates the weighting logic and provides progression direction signals.
 * Recovery Score: [-1.0, 1.0]
 *   - Negative: Reduce load (poor recovery)
 *   - Zero: Maintain load
 *   - Positive: Increase load (excellent recovery)
 */
public class RecoveryAssessment {

    private final Doms doms;
    private final Rpe lastRpe;
    private final SleepQuality sleep;
    private final ProgressionPolicy policy;
    private final double recoveryScore;
    private final LoadAdjustmentSignal signal;

    public RecoveryAssessment(Doms doms, Rpe lastRpe, SleepQuality sleep, ProgressionPolicy policy) {
        this.doms = doms;
        this.lastRpe = lastRpe;
        this.sleep = sleep;
        this.policy = policy;
        this.recoveryScore = calculateRecoveryScore();
        this.signal = determineSignal();
    }

    /**
     * Calculates overall recovery readiness [-1, 1] by combining three factors with policy weights.
     * DOMS has veto power: severe DOMS forces negative signal regardless of RPE/sleep.
     */
    private double calculateRecoveryScore() {
        // HARD CONSTRAINT: Severe DOMS veto
        if (doms.value() >= policy.severeDomsThreshold()) {
            return -0.8; // Strongly negative: reduce or mirror
        }

        // Moderate DOMS: significant penalty but not absolute
        double domsScore = 0.0;
        if (doms.value() >= policy.moderateDomsThreshold()) {
            // Map moderate DOMS [moderateThreshold, severeThreshold) to [-0.5, 0.0]
            int rangeSize = policy.severeDomsThreshold() - policy.moderateDomsThreshold();
            double proportionInRange = (double) (doms.value() - policy.moderateDomsThreshold()) / rangeSize;
            domsScore = -0.5 * proportionInRange;
        } else {
            // Mild/no DOMS: neutral to positive
            // Map [0, moderateThreshold) to [0.1, 0.0]
            domsScore = 0.1 * (1.0 - (double) doms.value() / policy.moderateDomsThreshold());
        }

        // RPE signal: lower RPE = more recovery capacity
        double rpeScore = 0.0;
        if (lastRpe.value() <= policy.lowRpeThreshold()) {
            rpeScore = 0.5; // Low RPE: ready to progress
        } else if (lastRpe.value() >= policy.optimalRpeMin() && lastRpe.value() <= policy.optimalRpeMax()) {
            rpeScore = 0.2; // Optimal RPE: maintain steady progression
        } else if (lastRpe.value() >= policy.highRpeThreshold()) {
            rpeScore = -0.4; // High RPE: need recovery
        } else {
            // Between optimal and high
            double proportionAboveOptimal = (lastRpe.value() - policy.optimalRpeMax()) /
                    (policy.highRpeThreshold() - policy.optimalRpeMax());
            rpeScore = 0.2 - (0.6 * proportionAboveOptimal);
        }

        // Sleep quality signal: recovery multiplier
        double sleepScore = sleep.recoveryFactor() - 1.0; // Normalize to [-0.5, 0.5]

        // Weighted combination (weights should sum to 1.0 for balance)
        // DOMS is heaviest (50%), RPE (30%), Sleep (20%)
        double totalWeight = policy.loadDecreaseWeightDoms() +
                           policy.loadIncreaseWeightRpe() +
                           policy.loadIncreaseWeightSleep();

        double normalized = (domsScore * policy.loadDecreaseWeightDoms() +
                           rpeScore * policy.loadIncreaseWeightRpe() +
                           sleepScore * policy.loadIncreaseWeightSleep()) / totalWeight;

        return Math.max(-1.0, Math.min(1.0, normalized));
    }

    /**
     * Translates recovery score into a concrete load adjustment signal.
     * Ensures DOMS-driven decisions take priority.
     */
    private LoadAdjustmentSignal determineSignal() {
        // HARD RULES: Severe DOMS always = reduce or mirror
        if (doms.value() >= policy.severeDomsThreshold()) {
            return LoadAdjustmentSignal.REDUCE_LOAD;
        }

        // Moderate DOMS + high RPE: definitely reduce
        if (doms.value() >= policy.moderateDomsThreshold() && lastRpe.value() >= policy.highRpeThreshold()) {
            return LoadAdjustmentSignal.MIRROR_SESSION;
        }

        // Moderate DOMS: lean toward mirror/reduce
        if (doms.value() >= policy.moderateDomsThreshold()) {
            return recoveryScore < -0.2 ? LoadAdjustmentSignal.REDUCE_LOAD : LoadAdjustmentSignal.MIRROR_SESSION;
        }

        // Mild recovery conditions: use weighted score.
        // With the current scoring ranges, >0.25 represents a true all-green session
        // (minimal DOMS, low/controlled RPE, and at least decent sleep quality).
        if (recoveryScore > 0.25) {
            return LoadAdjustmentSignal.INCREASE_LOAD;
        } else if (recoveryScore > 0.05) {
            return LoadAdjustmentSignal.STEADY_PROGRESSION;
        } else if (recoveryScore > -0.3) {
            return LoadAdjustmentSignal.MIRROR_SESSION;
        } else {
            return LoadAdjustmentSignal.REDUCE_LOAD;
        }
    }

    public double getRecoveryScore() {
        return recoveryScore;
    }

    public LoadAdjustmentSignal getSignal() {
        return signal;
    }

    public Doms getDoms() {
        return doms;
    }

    public Rpe getLastRpe() {
        return lastRpe;
    }

    public SleepQuality getSleep() {
        return sleep;
    }

    public enum LoadAdjustmentSignal {
        /** Reduce load by policy.minLoadReductionPct (severe recovery issues) */
        REDUCE_LOAD,

        /** Repeat last session exactly (moderate recovery issues) */
        MIRROR_SESSION,

        /** Maintain load, potentially increase reps or sets (good recovery) */
        STEADY_PROGRESSION,

        /** Increase load by microload increment (excellent recovery) */
        INCREASE_LOAD;

        public String description() {
            return switch (this) {
                case REDUCE_LOAD -> "Reduce load due to poor recovery";
                case MIRROR_SESSION -> "Mirror last session; adequate recovery";
                case STEADY_PROGRESSION -> "Steady progression; good recovery";
                case INCREASE_LOAD -> "Increase load; excellent recovery";
            };
        }
    }
}

