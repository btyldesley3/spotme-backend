package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.SetPrescription;
import com.spotme.domain.model.program.block.BlockId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkoutTest {

    @Test
    void createWorkoutWithValidInputsShouldSucceed() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        Workout workout = Workout.create(blockId, 1, 1, List.of(preset));

        assertThat(workout).isNotNull();
        assertThat(workout.blockId()).isEqualTo(blockId);
        assertThat(workout.weekNumber()).isEqualTo(1);
        assertThat(workout.sessionNumber()).isEqualTo(1);
        assertThat(workout.version()).isEqualTo(1);
        assertThat(workout.setPresets()).hasSize(1);
        assertThat(workout.exerciseIds()).containsExactly(exerciseId);
    }

    @Test
    void createWorkoutWithInvalidWeekNumberShouldFail() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        assertThatThrownBy(() -> Workout.create(blockId, 0, 1, List.of(preset)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weekNumber");

        assertThatThrownBy(() -> Workout.create(blockId, -1, 1, List.of(preset)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weekNumber");
    }

    @Test
    void createWorkoutWithInvalidSessionNumberShouldFail() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        assertThatThrownBy(() -> Workout.create(blockId, 1, 0, List.of(preset)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sessionNumber");
    }

    @Test
    void createWorkoutWithEmptySetPresetsShouldFail() {
        BlockId blockId = new BlockId(UUID.randomUUID());

        assertThatThrownBy(() -> Workout.create(blockId, 1, 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("setPresets");
    }

    @Test
    void createWorkoutWithNullSetPresetsShouldFail() {
        BlockId blockId = new BlockId(UUID.randomUUID());

        assertThatThrownBy(() -> Workout.create(blockId, 1, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("setPresets");
    }

    @Test
    void createNextVersionShouldIncrementVersion() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset1 = new SetPrescription(exerciseId, 1, 8, 60.0, false);
        SetPrescription preset2 = new SetPrescription(exerciseId, 1, 8, 62.5, false);

        Workout v1 = Workout.create(blockId, 1, 1, List.of(preset1));
        assertThat(v1.version()).isEqualTo(1);

        Workout v2 = Workout.createNextVersion(v1, List.of(preset2));
        assertThat(v2.version()).isEqualTo(2);
        assertThat(v2.blockId()).isEqualTo(blockId);
        assertThat(v2.weekNumber()).isEqualTo(v1.weekNumber());
        assertThat(v2.sessionNumber()).isEqualTo(v1.sessionNumber());
        assertThat(v2.workoutId()).isNotEqualTo(v1.workoutId());  // New ID for new version
    }

    @Test
    void setPresetsReturnsUnmodifiableList() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        Workout workout = Workout.create(blockId, 1, 1, List.of(preset));

        assertThatThrownBy(() -> workout.setPresets().add(preset))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void workoutEqualityBasedOnId() {
        WorkoutId id = WorkoutId.random();
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        Workout w1 = new Workout(id, blockId, 1, 1, 1, null, List.of(preset));
        Workout w2 = new Workout(id, blockId, 1, 1, 1, null, List.of(preset));
        Workout w3 = new Workout(WorkoutId.random(), blockId, 1, 1, 1, null, List.of(preset));

        assertThat(w1).isEqualTo(w2);
        assertThat(w1).isNotEqualTo(w3);
    }

    @Test
    void multipleExercisesInWorkout() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId ex1 = new ExerciseId(UUID.randomUUID());
        ExerciseId ex2 = new ExerciseId(UUID.randomUUID());
        ExerciseId ex3 = new ExerciseId(UUID.randomUUID());

        List<SetPrescription> presets = List.of(
                new SetPrescription(ex1, 1, 8, 60.0, false),
                new SetPrescription(ex1, 2, 8, 60.0, false),
                new SetPrescription(ex2, 1, 10, 50.0, false),
                new SetPrescription(ex3, 1, 5, 100.0, false)
        );

        Workout workout = Workout.create(blockId, 1, 1, presets);

        assertThat(workout.setPresets()).hasSize(4);
        assertThat(workout.exerciseIds()).containsExactlyInAnyOrder(ex1, ex2, ex3);
    }
}










