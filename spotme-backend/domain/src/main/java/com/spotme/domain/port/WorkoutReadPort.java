package com.spotme.domain.port;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.rules.ProgressionInput;

import java.util.List;
import java.util.Optional;

public interface WorkoutReadPort {
    Optional<ProgressionInput> lastProgressionInput(UserId userId, ExerciseId exerciseId);

    Optional<WorkoutSession> findSession(UserId userId, WorkoutSessionId sessionId);

    Optional<WorkoutSession> findLatestSession(UserId userId);

    List<WorkoutSession> listSessionsFor(UserId userId, int limit);
}
