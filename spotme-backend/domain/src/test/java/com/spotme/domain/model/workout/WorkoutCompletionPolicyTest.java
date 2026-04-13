package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Rpe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutCompletionPolicyTest {

    @Test
    void marksSessionCompletedButNotProgressionEligibleWhenRecoveryIsRequiredAndMissing() {
        var exerciseId = ExerciseId.random();
        var setsByExercise = Map.of(
                exerciseId,
                List.of(new SetEntry(1, 8, 60.0, new Rpe(8.0), "top set"))
        );

        var decision = WorkoutCompletionPolicy.permissive().evaluate(setsByExercise, false);

        assertThat(decision.completed()).isTrue();
        assertThat(decision.allowsProgression()).isFalse();
        assertThat(decision.reason()).isEqualTo(WorkoutCompletionDecision.Reason.RECOVERY_FEEDBACK_MISSING);
    }

    @Test
    void failsWhenAnyExerciseIsBelowMinSetRequirement() {
        var exerciseA = ExerciseId.random();
        var exerciseB = ExerciseId.random();
        var policy = new WorkoutCompletionPolicy(3, 2, 2, false);

        var setsByExercise = Map.of(
                exerciseA, List.of(
                        new SetEntry(1, 8, 60.0, new Rpe(8.0), "a1"),
                        new SetEntry(2, 8, 62.5, new Rpe(8.5), "a2")
                ),
                exerciseB, List.of(new SetEntry(1, 10, 30.0, new Rpe(7.0), "b1"))
        );

        var decision = policy.evaluate(setsByExercise, true);

        assertThat(decision.completed()).isFalse();
        assertThat(decision.allowsProgression()).isFalse();
        assertThat(decision.reason()).isEqualTo(WorkoutCompletionDecision.Reason.INSUFFICIENT_SETS_FOR_EXERCISE);
    }
}

