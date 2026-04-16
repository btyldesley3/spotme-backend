package com.spotme.adapters.out.persistence;

import com.spotme.adapters.out.persistence.jpa.WorkoutJpaRepository;
import com.spotme.adapters.out.persistence.mapper.WorkoutMapper;
import com.spotme.domain.model.program.block.BlockId;
import com.spotme.domain.model.workout.Workout;
import com.spotme.domain.model.workout.WorkoutId;
import com.spotme.domain.port.WorkoutTemplateReadPort;
import com.spotme.domain.port.WorkoutTemplateWritePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class PostgresWorkoutTemplateAdapter implements WorkoutTemplateReadPort, WorkoutTemplateWritePort {

    private final WorkoutJpaRepository workoutRepository;
    private final WorkoutMapper workoutMapper;

    public PostgresWorkoutTemplateAdapter(WorkoutJpaRepository workoutRepository, WorkoutMapper workoutMapper) {
        this.workoutRepository = workoutRepository;
        this.workoutMapper = workoutMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Workout> findById(WorkoutId workoutId) {
        return workoutRepository.findById(workoutId.value())
                .map(workoutMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workout> findByBlockId(BlockId blockId) {
        UUID blockUuid = blockId.value();
        return workoutRepository.findByBlockIdOrderByWeekNumberAscSessionNumberAsc(blockUuid).stream()
                .map(workoutMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Workout> listWorkoutsInBlock(BlockId blockId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        UUID blockUuid = blockId.value();
        return workoutRepository
                .findByBlockIdOrderByWeekNumberAscSessionNumberAsc(blockUuid, PageRequest.of(0, limit))
                .stream()
                .map(workoutMapper::toDomain)
                .toList();
    }

    @Override
    public void save(Workout workout) {
        var entity = workoutMapper.toEntity(workout);
        workoutRepository.save(entity);
    }

    @Override
    public void delete(Workout workout) {
        workoutRepository.deleteById(workout.workoutId().value());
    }
}


