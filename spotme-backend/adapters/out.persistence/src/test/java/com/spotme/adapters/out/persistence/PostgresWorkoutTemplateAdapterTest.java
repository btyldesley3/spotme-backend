package com.spotme.adapters.out.persistence;

import com.spotme.adapters.out.persistence.mapper.WorkoutMapper;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.SetPrescription;
import com.spotme.domain.model.program.block.BlockId;
import com.spotme.domain.model.workout.Workout;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PostgresWorkoutTemplateAdapterTest.TestApplication.class)
@Import({
        PostgresWorkoutTemplateAdapter.class,
        WorkoutMapper.class,
        PostgresWorkoutTemplateAdapterTest.EmbeddedPostgresConfig.class
})
class PostgresWorkoutTemplateAdapterTest {

    @Autowired
    private PostgresWorkoutTemplateAdapter adapter;

    @Test
    void saveAndFindWorkoutById() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        Workout workout = Workout.create(blockId, 1, 1, List.of(preset));
        adapter.save(workout);

        var found = adapter.findById(workout.workoutId());

        assertThat(found).isPresent();
        assertThat(found.get().workoutId()).isEqualTo(workout.workoutId());
        assertThat(found.get().blockId()).isEqualTo(blockId);
        assertThat(found.get().weekNumber()).isEqualTo(1);
        assertThat(found.get().sessionNumber()).isEqualTo(1);
        assertThat(found.get().version()).isEqualTo(1);
        assertThat(found.get().setPresets()).hasSize(1);
    }

    @Test
    void findByBlockIdReturnsAllWorkoutsInBlock() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId ex1 = new ExerciseId(UUID.randomUUID());
        ExerciseId ex2 = new ExerciseId(UUID.randomUUID());

        SetPrescription p1 = new SetPrescription(ex1, 1, 8, 60.0, false);
        SetPrescription p2 = new SetPrescription(ex2, 1, 10, 50.0, false);

        Workout w1 = Workout.create(blockId, 1, 1, List.of(p1));
        Workout w2 = Workout.create(blockId, 1, 2, List.of(p2));
        Workout w3 = Workout.create(new BlockId(UUID.randomUUID()), 1, 1, List.of(p1));

        adapter.save(w1);
        adapter.save(w2);
        adapter.save(w3);

        var foundInBlock = adapter.findByBlockId(blockId);

        assertThat(foundInBlock).hasSize(2);
        assertThat(foundInBlock).extracting(Workout::sessionNumber).containsExactly(1, 2);
    }

    @Test
    void findByBlockIdReturnsSortedByWeekAndSession() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        Workout w1 = Workout.create(blockId, 2, 1, List.of(preset));
        Workout w2 = Workout.create(blockId, 1, 2, List.of(preset));
        Workout w3 = Workout.create(blockId, 1, 1, List.of(preset));

        adapter.save(w1);
        adapter.save(w2);
        adapter.save(w3);

        var found = adapter.findByBlockId(blockId);

        assertThat(found)
                .hasSize(3)
                .extracting(w -> w.weekNumber() + ":" + w.sessionNumber())
                .containsExactly("1:1", "1:2", "2:1");
    }

    @Test
    void listWorkoutsInBlockWithLimit() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        Workout w1 = Workout.create(blockId, 1, 1, List.of(preset));
        Workout w2 = Workout.create(blockId, 1, 2, List.of(preset));
        Workout w3 = Workout.create(blockId, 2, 1, List.of(preset));

        adapter.save(w1);
        adapter.save(w2);
        adapter.save(w3);

        var found = adapter.listWorkoutsInBlock(blockId, 2);

        assertThat(found).hasSize(2);
    }

    @Test
    void workoutMapperBidirectionalityPreservesData() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId ex1 = new ExerciseId(UUID.randomUUID());
        ExerciseId ex2 = new ExerciseId(UUID.randomUUID());

        List<SetPrescription> presets = List.of(
                new SetPrescription(ex1, 1, 8, 60.0, false),
                new SetPrescription(ex1, 2, 8, 62.5, false),
                new SetPrescription(ex2, 1, 10, 50.0, true)
        );

        Workout original = Workout.create(blockId, 2, 3, presets);
        adapter.save(original);

        var reloaded = adapter.findById(original.workoutId()).orElseThrow();

        assertThat(reloaded.workoutId()).isEqualTo(original.workoutId());
        assertThat(reloaded.blockId()).isEqualTo(original.blockId());
        assertThat(reloaded.weekNumber()).isEqualTo(original.weekNumber());
        assertThat(reloaded.sessionNumber()).isEqualTo(original.sessionNumber());
        assertThat(reloaded.version()).isEqualTo(original.version());
        assertThat(reloaded.notes()).isEqualTo(original.notes());
        assertThat(reloaded.setPresets()).hasSize(3);

        // Verify each prescription
        List<SetPrescription> reloadedPresets = reloaded.setPresets();
        assertThat(reloadedPresets.get(0))
                .extracting(SetPrescription::order, SetPrescription::prescribedReps, SetPrescription::prescribedWeightKg, SetPrescription::backoff)
                .containsExactly(1, 8, 60.0, false);
        assertThat(reloadedPresets.get(2))
                .extracting(SetPrescription::backoff)
                .isEqualTo(true);
    }

    @Test
    void deleteWorkout() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription preset = new SetPrescription(exerciseId, 1, 8, 60.0, false);

        Workout workout = Workout.create(blockId, 1, 1, List.of(preset));
        adapter.save(workout);

        assertThat(adapter.findById(workout.workoutId())).isPresent();

        adapter.delete(workout);

        assertThat(adapter.findById(workout.workoutId())).isEmpty();
    }

    @Test
    void createNextVersionAndBothExist() {
        BlockId blockId = new BlockId(UUID.randomUUID());
        ExerciseId exerciseId = new ExerciseId(UUID.randomUUID());
        SetPrescription p1 = new SetPrescription(exerciseId, 1, 8, 60.0, false);
        SetPrescription p2 = new SetPrescription(exerciseId, 1, 8, 62.5, false);

        Workout v1 = Workout.create(blockId, 1, 1, List.of(p1));
        adapter.save(v1);

        Workout v2 = Workout.createNextVersion(v1, List.of(p2));
        adapter.save(v2);

        var foundV1 = adapter.findById(v1.workoutId());
        var foundV2 = adapter.findById(v2.workoutId());

        assertThat(foundV1).isPresent();
        assertThat(foundV2).isPresent();
        assertThat(foundV1.get().version()).isEqualTo(1);
        assertThat(foundV2.get().version()).isEqualTo(2);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmbeddedPostgresConfig {

        @Bean(destroyMethod = "close")
        EmbeddedPostgres embeddedPostgres() throws IOException {
            return EmbeddedPostgres.builder().start();
        }

        @Bean
        DataSource dataSource(EmbeddedPostgres postgres) {
            return postgres.getPostgresDatabase();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}










