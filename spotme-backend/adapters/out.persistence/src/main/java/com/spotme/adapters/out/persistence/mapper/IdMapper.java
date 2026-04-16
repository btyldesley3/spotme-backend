package com.spotme.adapters.out.persistence.mapper;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.program.block.BlockId;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutId;

import java.util.UUID;

public class IdMapper {
    public static UUID toUuid(UserId id) {
        return id.value();
    }

    public static UUID toUuid(ExerciseId id) {
        return id.value();
    }

    public static UUID toUuid(WorkoutId id) {
        return id.value();
    }

    public static UUID toUuid(BlockId id) {
        return id.value();
    }

    public static UserId toUserId(UUID u) {
        return new UserId(u);
    }

    public static ExerciseId toExerciseId(UUID u) {
        return new ExerciseId(u);
    }

    public static WorkoutId toWorkoutId(UUID u) {
        return new WorkoutId(u);
    }

    public static BlockId toBlockId(UUID u) {
        return new BlockId(u);
    }
}
