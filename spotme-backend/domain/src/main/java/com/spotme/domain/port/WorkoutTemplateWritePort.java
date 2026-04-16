package com.spotme.domain.port;

import com.spotme.domain.model.workout.Workout;

/**
 * Port for writing Workout templates.
 * Workouts are immutable once created; typically operations are create and delete (via versioning).
 */
public interface WorkoutTemplateWritePort {
    void save(Workout workout);

    void delete(Workout workout);
}

