package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutSessionTest {

    @Test
    void derivesProgressionInputFromHeaviestCompletedTopSetAndRecovery() {
        var userId = UserId.random();
        var exerciseId = ExerciseId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-01T10:00:00Z"));

        session.addSet(exerciseId, new SetEntry(1, 10, 55.0, new Rpe(7.5), "first working set"));
        session.addSet(exerciseId, new SetEntry(2, 8, 60.0, new Rpe(8.5), "heaviest set"));
        session.finish(Instant.parse("2026-04-01T10:45:00Z"));
        session.reportRecovery(new Doms(3), new SleepQuality(8));

        var progressionInput = session.progressionInputFor(exerciseId).orElseThrow();

        assertThat(progressionInput.lastTopSetWeightKg()).isEqualTo(60.0);
        assertThat(progressionInput.lastTopSetReps()).isEqualTo(8);
        assertThat(progressionInput.lastTopSetRpe().value()).isEqualTo(8.5);
        assertThat(progressionInput.doms().value()).isEqualTo(3);
        assertThat(progressionInput.sleepQuality().value()).isEqualTo(8);
    }

    @Test
    void breaksTopSetTiesUsingHigherRpeThenLaterSetNumber() {
        var userId = UserId.random();
        var exerciseId = ExerciseId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-02T10:00:00Z"));

        session.addSet(exerciseId, new SetEntry(1, 8, 60.0, new Rpe(8.0), "first top-set candidate"));
        session.addSet(exerciseId, new SetEntry(2, 8, 60.0, new Rpe(8.5), "same load, higher effort"));
        session.finish(Instant.parse("2026-04-02T10:40:00Z"));
        session.reportRecovery(new Doms(2), new SleepQuality(7));

        var progressionInput = session.progressionInputFor(exerciseId).orElseThrow();

        assertThat(progressionInput.lastTopSetWeightKg()).isEqualTo(60.0);
        assertThat(progressionInput.lastTopSetRpe().value()).isEqualTo(8.5);
        assertThat(progressionInput.lastTopSetReps()).isEqualTo(8);
    }

    @Test
    void requiresSequentialSetNumbersPerExercise() {
        var session = WorkoutSession.start(UserId.random(), Instant.parse("2026-04-03T10:00:00Z"));
        var exerciseId = ExerciseId.random();

        session.addSet(exerciseId, new SetEntry(1, 8, 50.0, new Rpe(7.0), "first"));

        assertThatThrownBy(() -> session.addSet(exerciseId, new SetEntry(3, 8, 52.5, new Rpe(8.0), "skipped set number")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Set numbers must be sequential");
    }

    @Test
    void cannotReportRecoveryBeforeSessionIsFinished() {
        var session = WorkoutSession.start(UserId.random(), Instant.parse("2026-04-04T10:00:00Z"));

        assertThatThrownBy(() -> session.reportRecovery(new Doms(4), new SleepQuality(6)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be finished");
    }

    @Test
    void returnsEmptyProgressionInputWhenRecoveryFeedbackIsMissing() {
        var exerciseId = ExerciseId.random();
        var session = WorkoutSession.start(UserId.random(), Instant.parse("2026-04-05T10:00:00Z"));
        session.addSet(exerciseId, new SetEntry(1, 8, 50.0, new Rpe(7.5), "working set"));
        session.finish(Instant.parse("2026-04-05T10:30:00Z"));

        assertThat(session.progressionInputFor(exerciseId)).isEmpty();
    }

    @Test
    void blocksProgressionInputWhenCompletionPolicyIsNotMet() {
        var exerciseId = ExerciseId.random();
        var session = WorkoutSession.start(UserId.random(), Instant.parse("2026-04-06T10:00:00Z"));
        session.addSet(exerciseId, new SetEntry(1, 8, 60.0, new Rpe(8.0), "single set"));

        var strictPolicy = new WorkoutCompletionPolicy(2, 1, 2, true);
        session.finish(Instant.parse("2026-04-06T10:30:00Z"), strictPolicy);
        session.reportRecovery(new Doms(2), new SleepQuality(7));

        assertThat(session.completionDecision().completed()).isFalse();
        assertThat(session.progressionInputFor(exerciseId)).isEmpty();
    }
}

