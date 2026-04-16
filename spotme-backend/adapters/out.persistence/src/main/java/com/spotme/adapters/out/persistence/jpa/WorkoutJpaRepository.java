package com.spotme.adapters.out.persistence.jpa;

import com.spotme.adapters.out.persistence.jpa.entity.WorkoutEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutJpaRepository extends CrudRepository<WorkoutEntity, UUID> {
    List<WorkoutEntity> findByBlockIdOrderByWeekNumberAscSessionNumberAsc(UUID blockId);

    List<WorkoutEntity> findByBlockIdOrderByWeekNumberAscSessionNumberAsc(UUID blockId, Pageable pageable);

    Optional<WorkoutEntity> findByIdAndBlockId(UUID workoutId, UUID blockId);
}



