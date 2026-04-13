package com.spotme.domain.model.workout;

/**
 * Domain decision that separates workout completion from progression eligibility.
 */
public record WorkoutCompletionDecision(
        boolean completed,
        boolean allowsProgression,
        Reason reason
) {
    public WorkoutCompletionDecision {
        if (reason == null) {
            throw new IllegalArgumentException("reason cannot be null");
        }
    }

    public static WorkoutCompletionDecision inProgress() {
        return new WorkoutCompletionDecision(false, false, Reason.IN_PROGRESS);
    }

    public enum Reason {
        IN_PROGRESS,
        INSUFFICIENT_TOTAL_SETS,
        INSUFFICIENT_EXERCISE_VARIETY,
        INSUFFICIENT_SETS_FOR_EXERCISE,
        RECOVERY_FEEDBACK_MISSING,
        COMPLETED
    }
}

