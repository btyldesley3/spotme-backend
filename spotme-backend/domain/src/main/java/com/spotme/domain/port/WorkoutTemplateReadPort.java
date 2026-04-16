package com.spotme.domain.port;

import com.spotme.domain.model.program.block.BlockId;
import com.spotme.domain.model.workout.Workout;
import com.spotme.domain.model.workout.WorkoutId;

import java.util.List;
import java.util.Optional;

/**
 * Port for reading Workout templates.
 * Workouts are immutable blueprint aggregates within training blocks.
 */
public interface WorkoutTemplateReadPort {
    Optional<Workout> findById(WorkoutId workoutId);

    List<Workout> findByBlockId(BlockId blockId);

    List<Workout> listWorkoutsInBlock(BlockId blockId, int limit);
}

