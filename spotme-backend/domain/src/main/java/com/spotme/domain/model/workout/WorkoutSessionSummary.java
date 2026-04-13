package com.spotme.domain.model.workout;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.rules.RecoveryAssessment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record WorkoutSessionSummary(
        WorkoutSessionId sessionId,
        UserId userId,
        Instant startedAt,
        Optional<Instant> finishedAt,
        int totalExercises,
        int totalSets,
        int totalReps,
        double totalVolumeKg,
        WorkoutCompletionDecision completionDecision,
        Optional<RecoveryAssessment.LoadAdjustmentSignal> recommendedLoadSignal,
        List<ExerciseSessionSummary> exercises
) {
}

