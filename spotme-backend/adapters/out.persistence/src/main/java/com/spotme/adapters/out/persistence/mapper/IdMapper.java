package com.spotme.adapters.out.persistence.mapper;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.user.UserId;

import java.util.UUID;

public class IdMapper {
    public static UUID toUuid(UserId id) {
        return id.value();
    }

    public static UUID toUuid(ExerciseId id) {
        return id.value();
    }

    public static UserId toUserId(UUID u) {
        return new UserId(u);
    }

    public static ExerciseId toExerciseId(UUID u) {
        return new ExerciseId(u);
    }
}
