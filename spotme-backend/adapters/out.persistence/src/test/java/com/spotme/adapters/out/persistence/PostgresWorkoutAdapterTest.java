package com.spotme.adapters.out.persistence;

import com.spotme.adapters.out.persistence.jpa.UserJpaRepository;
import com.spotme.adapters.out.persistence.jpa.WorkoutSetJpaRepository;
import com.spotme.adapters.out.persistence.jpa.entity.UserEntity;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.SetEntry;
import com.spotme.domain.model.workout.WorkoutSession;
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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = PostgresWorkoutAdapterTest.TestApplication.class)
@Import({PostgresWorkoutAdapter.class, PostgresWorkoutAdapterTest.EmbeddedPostgresConfig.class})
class PostgresWorkoutAdapterTest {

    @Autowired
    private PostgresWorkoutAdapter adapter;

    @Autowired
    private UserJpaRepository users;

    @Autowired
    private WorkoutSetJpaRepository sets;

    @Test
    void saveSessionUpdatesExistingWorkoutSetsWithoutDuplicatingNaturalKeys() {
        UUID userUuid = UUID.randomUUID();
        UserId userId = new UserId(userUuid);
        ExerciseId exerciseId = new ExerciseId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        users.save(userEntity(userUuid));

        WorkoutSession firstDraft = WorkoutSession.start(userId, Instant.parse("2026-04-16T08:00:00Z"));
        firstDraft.addSet(exerciseId, new SetEntry(1, 8, 57.5, new Rpe(7.0), "first working set"));
        adapter.saveSession(firstDraft);

        WorkoutSession reloaded = adapter.findSession(userId, firstDraft.sessionId()).orElseThrow();
        reloaded.addSet(exerciseId, new SetEntry(2, 8, 60.0, new Rpe(6.5), "top set"));
        adapter.saveSession(reloaded);

        var persistedSets = sets.findBySessionIdOrderByExerciseIdAscSetNumberAsc(firstDraft.sessionId().value());
        assertThat(persistedSets)
                .hasSize(2)
                .extracting(set -> set.getExerciseId().toString() + ":" + set.getSetNumber())
                .containsExactly(
                        exerciseId.value() + ":1",
                        exerciseId.value() + ":2"
                );

        WorkoutSession persistedSession = adapter.findSession(userId, firstDraft.sessionId()).orElseThrow();
        assertThat(persistedSession.sets()).containsKey(exerciseId);
        assertThat(persistedSession.sets().get(exerciseId)).hasSize(2);
    }

    private UserEntity userEntity(UUID userId) {
        var entity = new UserEntity();
        entity.setId(userId);
        entity.setExperienceLevel(ExperienceLevel.BEGINNER);
        entity.setTrainingGoal(TrainingGoal.STRENGTH);
        entity.setBaselineSleepHours(7);
        entity.setStressSensitivity(3);
        return entity;
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



