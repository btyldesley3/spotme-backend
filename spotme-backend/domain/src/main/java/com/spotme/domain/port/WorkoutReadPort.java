package com.spotme.domain.port;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.rules.ProgressionInput;

import java.util.Optional;

public interface WorkoutReadPort {
    Optional<ProgressionInput> lastProgressionInput(UserId userId, ExerciseId exerciseId);
}
