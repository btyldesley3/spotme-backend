package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.SetPrescription;
import com.spotme.domain.model.program.block.BlockId;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a designed workout template/blueprint.
 * Contains prescribed exercises and set targets for a specific session within a training block.
 * Immutable once created; used as a template for execution (WorkoutSession).
 */
public class Workout {

    private final WorkoutId workoutId;
    private final BlockId blockId;
    private final int weekNumber;
    private final int sessionNumber;
    private final int version;
    private final String notes;
    private final List<SetPrescription> setPresets;

    public Workout(
            WorkoutId workoutId,
            BlockId blockId,
            int weekNumber,
            int sessionNumber,
            int version,
            String notes,
            List<SetPrescription> setPresets
    ) {
        this.workoutId = Objects.requireNonNull(workoutId, "workoutId");
        this.blockId = Objects.requireNonNull(blockId, "blockId");
        this.weekNumber = validatePositive(weekNumber, "weekNumber");
        this.sessionNumber = validatePositive(sessionNumber, "sessionNumber");
        this.version = validateNonNegative(version, "version");
        this.notes = notes;
        this.setPresets = validateNotEmpty(setPresets, "setPresets");
    }

    /**
     * Factory method to create a new Workout template.
     */
    public static Workout create(
            BlockId blockId,
            int weekNumber,
            int sessionNumber,
            List<SetPrescription> setPresets
    ) {
        return new Workout(
                WorkoutId.random(),
                blockId,
                weekNumber,
                sessionNumber,
                1,  // Initial version
                null,  // No notes by default
                setPresets
        );
    }

    /**
     * Factory method to create a new version of an existing Workout (copy-on-create).
     */
    public static Workout createNextVersion(Workout existing, List<SetPrescription> updatedPresets) {
        return new Workout(
                WorkoutId.random(),  // New ID for versioned workout
                existing.blockId,
                existing.weekNumber,
                existing.sessionNumber,
                existing.version + 1,
                existing.notes,
                updatedPresets
        );
    }

    // Accessors

    public WorkoutId workoutId() {
        return workoutId;
    }

    public BlockId blockId() {
        return blockId;
    }

    public int weekNumber() {
        return weekNumber;
    }

    public int sessionNumber() {
        return sessionNumber;
    }

    public int version() {
        return version;
    }

    public String notes() {
        return notes;
    }

    public List<SetPrescription> setPresets() {
        return Collections.unmodifiableList(setPresets);
    }

    public Collection<ExerciseId> exerciseIds() {
        return setPresets.stream()
                .map(SetPrescription::exerciseId)
                .distinct()
                .toList();
    }

    // Helpers

    private static int validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive, got " + value);
        }
        return value;
    }

    private static int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative, got " + value);
        }
        return value;
    }

    private static <T extends Collection<?>> T validateNotEmpty(T collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return collection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Workout workout = (Workout) o;
        return workoutId.equals(workout.workoutId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workoutId);
    }

    @Override
    public String toString() {
        return "Workout{" +
                "workoutId=" + workoutId +
                ", blockId=" + blockId +
                ", weekNumber=" + weekNumber +
                ", sessionNumber=" + sessionNumber +
                ", version=" + version +
                ", setPresets=" + setPresets.size() +
                '}';
    }
}

