package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.rules.ProgressionPolicy;
import com.spotme.domain.rules.RecoveryAssessment;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutSessionSummaryTest {

    @Test
    void buildsSessionAndExerciseAggregatesWithRecommendedSignal() {
        var userId = UserId.random();
        var bench = ExerciseId.random();
        var row = ExerciseId.random();

        var session = WorkoutSession.start(userId, Instant.parse("2026-04-12T10:00:00Z"));
        session.addSet(bench, new SetEntry(1, 8, 60.0, new Rpe(7.5), "bench-1"));
        session.addSet(bench, new SetEntry(2, 8, 62.5, new Rpe(6.5), "bench-2"));
        session.addSet(row, new SetEntry(1, 10, 45.0, new Rpe(7.0), "row-1"));
        session.finish(Instant.parse("2026-04-12T10:55:00Z"));
        session.reportRecovery(new Doms(0), new SleepQuality(9));

        var policy = new ProgressionPolicy(
                1.25,
                7,
                4,
                7.0,
                8.0,
                9.0,
                9.5,
                4,
                0.3,
                0.2,
                0.5,
                2.5
        );

        var summary = session.summary(policy);

        assertThat(summary.totalExercises()).isEqualTo(2);
        assertThat(summary.totalSets()).isEqualTo(3);
        assertThat(summary.totalReps()).isEqualTo(26);
        assertThat(summary.totalVolumeKg()).isEqualTo((8 * 60.0) + (8 * 62.5) + (10 * 45.0));
        assertThat(summary.completionDecision().completed()).isTrue();
        assertThat(summary.completionDecision().allowsProgression()).isTrue();
        assertThat(summary.recommendedLoadSignal())
                .contains(RecoveryAssessment.LoadAdjustmentSignal.INCREASE_LOAD);

        var benchSummary = summary.exercises().stream()
                .filter(exercise -> exercise.exerciseId().equals(bench))
                .findFirst()
                .orElseThrow();

        assertThat(benchSummary.totalSets()).isEqualTo(2);
        assertThat(benchSummary.topSet().weightKg()).isEqualTo(62.5);
    }
}


