package com.spotme.adapters.out.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotme.adapters.out.persistence.jpa.entity.WorkoutEntity;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.SetPrescription;
import com.spotme.domain.model.program.block.BlockId;
import com.spotme.domain.model.workout.Workout;
import com.spotme.domain.model.workout.WorkoutId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class WorkoutMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Map domain Workout to JPA entity.
     */
    public WorkoutEntity toEntity(Workout workout) {
        String setPresetsJson = serializeSetPrescriptions(workout.setPresets());
        return new WorkoutEntity(
                IdMapper.toUuid(workout.workoutId()),
                IdMapper.toUuid(workout.blockId()),
                workout.weekNumber(),
                workout.sessionNumber(),
                workout.version(),
                workout.notes(),
                setPresetsJson
        );
    }

    /**
     * Map JPA entity to domain Workout.
     */
    public Workout toDomain(WorkoutEntity entity) {
        List<SetPrescription> setPresets = deserializeSetPrescriptions(entity.getSetPresetsJson());
        return new Workout(
                new WorkoutId(entity.getId()),
                new BlockId(entity.getBlockId()),
                entity.getWeekNumber(),
                entity.getSessionNumber(),
                entity.getVersion(),
                entity.getNotes(),
                setPresets
        );
    }

    private String serializeSetPrescriptions(List<SetPrescription> prescriptions) {
        try {
            List<SetPrescriptionJson> jsonPayload = prescriptions.stream()
                    .map(SetPrescriptionJson::new)
                    .toList();
            return objectMapper.writeValueAsString(jsonPayload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize set prescriptions", e);
        }
    }

    private List<SetPrescription> deserializeSetPrescriptions(String json) {
        try {
            List<SetPrescriptionJson> prescriptionJsons = objectMapper.readValue(
                    json,
                    new TypeReference<List<SetPrescriptionJson>>() {}
            );
            return prescriptionJsons.stream()
                    .map(pj -> new SetPrescription(
                            new ExerciseId(UUID.fromString(pj.exerciseId)),
                            pj.order,
                            pj.prescribedReps,
                            pj.prescribedWeightKg,
                            pj.backoff
                    ))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize set prescriptions", e);
        }
    }

    /**
     * JSON representation of SetPrescription for serialization.
     * (Using inner class to avoid polluting domain model)
     */
    public static class SetPrescriptionJson {
        public String exerciseId;
        public int order;
        public int prescribedReps;
        public double prescribedWeightKg;
        public boolean backoff;

        // Default constructor for Jackson
        public SetPrescriptionJson() {
        }

        public SetPrescriptionJson(SetPrescription sp) {
            this.exerciseId = sp.exerciseId().value().toString();
            this.order = sp.order();
            this.prescribedReps = sp.prescribedReps();
            this.prescribedWeightKg = sp.prescribedWeightKg();
            this.backoff = sp.backoff();
        }
    }
}




