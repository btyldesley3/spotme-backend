package com.spotme.domain.model.exercise;

import java.util.Optional;

public interface ExerciseRepository {
    Optional<Exercise> findById(String exerciseId);
    void save(Exercise exercise);
}
