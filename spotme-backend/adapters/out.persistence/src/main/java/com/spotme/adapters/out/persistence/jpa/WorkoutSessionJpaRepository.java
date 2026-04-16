package com.spotme.adapters.out.persistence.jpa;

import com.spotme.adapters.out.persistence.jpa.entity.WorkoutSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutSessionJpaRepository extends JpaRepository<WorkoutSessionEntity, UUID> {
    Optional<WorkoutSessionEntity> findByIdAndUserId(UUID id, UUID userId);

    List<WorkoutSessionEntity> findByUserId(UUID userId);
}

