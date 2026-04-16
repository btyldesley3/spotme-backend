package com.spotme.adapters.out.persistence.jpa;

import com.spotme.adapters.out.persistence.jpa.entity.WorkoutSetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkoutSetJpaRepository extends JpaRepository<WorkoutSetEntity, Long> {
    List<WorkoutSetEntity> findBySessionIdOrderByExerciseIdAscSetNumberAsc(UUID sessionId);

    void deleteBySessionId(UUID sessionId);
}

