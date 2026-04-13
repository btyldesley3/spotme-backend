package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;

import java.util.List;
import java.util.Map;

/**
 * Completion policy for a workout session.
 */
public record WorkoutCompletionPolicy(
        int minTotalSets,
        int minDistinctExercises,
        int minSetsPerExercise,
        boolean requireRecoveryFeedbackForProgression
) {
    public WorkoutCompletionPolicy {
        if (minTotalSets < 1) {
            throw new IllegalArgumentException("minTotalSets must be at least 1");
        }
        if (minDistinctExercises < 1) {
            throw new IllegalArgumentException("minDistinctExercises must be at least 1");
        }
        if (minSetsPerExercise < 1) {
            throw new IllegalArgumentException("minSetsPerExercise must be at least 1");
        }
    }

    public static WorkoutCompletionPolicy permissive() {
        return new WorkoutCompletionPolicy(1, 1, 1, true);
    }

    public WorkoutCompletionDecision evaluate(Map<ExerciseId, List<SetEntry>> setsByExercise, boolean hasRecoveryFeedback) {
        int totalSets = setsByExercise.values().stream().mapToInt(List::size).sum();
        if (totalSets < minTotalSets) {
            return new WorkoutCompletionDecision(false, false, WorkoutCompletionDecision.Reason.INSUFFICIENT_TOTAL_SETS);
        }

        if (setsByExercise.size() < minDistinctExercises) {
            return new WorkoutCompletionDecision(false, false, WorkoutCompletionDecision.Reason.INSUFFICIENT_EXERCISE_VARIETY);
        }

        boolean anyExerciseBelowMinSets = setsByExercise.values().stream().anyMatch(sets -> sets.size() < minSetsPerExercise);
        if (anyExerciseBelowMinSets) {
            return new WorkoutCompletionDecision(false, false, WorkoutCompletionDecision.Reason.INSUFFICIENT_SETS_FOR_EXERCISE);
        }

        if (requireRecoveryFeedbackForProgression && !hasRecoveryFeedback) {
            return new WorkoutCompletionDecision(true, false, WorkoutCompletionDecision.Reason.RECOVERY_FEEDBACK_MISSING);
        }

        return new WorkoutCompletionDecision(true, true, WorkoutCompletionDecision.Reason.COMPLETED);
    }
}

