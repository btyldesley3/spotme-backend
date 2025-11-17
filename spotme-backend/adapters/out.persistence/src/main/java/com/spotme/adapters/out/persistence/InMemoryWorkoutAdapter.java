package com.spotme.adapters.out.persistence;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.spotme.domain.rules.ProgressionInput;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryWorkoutAdapter implements WorkoutReadPort, WorkoutWritePort {

    private final Map<UserId, Map<ExerciseId, ProgressionInput>> inputs = new HashMap<>();
    private final Map<UserId, List<Prescription>> prescriptions = new HashMap<>();

    public InMemoryWorkoutAdapter() {
        // Seed with a default input (bench press example)
        var userId = UserId.random();
        var exerciseId = ExerciseId.random();
        inputs.put(userId, Map.of(exerciseId, new ProgressionInput(60, 8, new Rpe(8.5), new Doms(3))));
    }

    @Override
    public Optional<ProgressionInput> lastProgressionInput(UserId userId, ExerciseId exerciseId) {
        return Optional.ofNullable(inputs.getOrDefault(userId, Collections.emptyMap()).get(exerciseId));
    }

    @Override
    public void savePrescription(UserId userId, Prescription prescription) {
        prescriptions.computeIfAbsent(userId, k -> new ArrayList<>()).add(prescription);
    }
}
