package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.WorkoutReadPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetLatestWorkoutSessionTest {

    @Test
    void returnsLatestCompletedSession() {
        var userId = UserId.random();
        var workoutPort = createMockWorkoutPort(userId, 2);

        var useCase = new GetLatestWorkoutSession(workoutPort);
        var result = useCase.handle(new GetLatestWorkoutSession.Command(userId.toString()));

        assertEquals(userId, result.summary().userId());
    }

    @Test
    void throwsWhenNoSessionExists() {
        var userId = UserId.random();
        var workoutPort = new StubWorkoutReadPort(Map.of());

        var useCase = new GetLatestWorkoutSession(workoutPort);

        assertThrows(java.util.NoSuchElementException.class, () ->
                useCase.handle(new GetLatestWorkoutSession.Command(userId.toString()))
        );
    }

    private WorkoutReadPort createMockWorkoutPort(UserId userId, int sessionCount) {
        return new StubWorkoutReadPort(buildSessionMap(userId, sessionCount));
    }

    private Map<UserId, List<com.spotme.domain.model.workout.WorkoutSession>> buildSessionMap(UserId userId, int count) {
        var sessions = new java.util.ArrayList<com.spotme.domain.model.workout.WorkoutSession>();
        for (int i = 0; i < count; i++) {
            var session = com.spotme.domain.model.workout.WorkoutSession.start(
                    userId,
                    Instant.parse(String.format("2026-04-10T%02d:00:00Z", 9 + i))
            );
            session.addSet(
                    com.spotme.domain.model.exercise.ExerciseId.random(),
                    new com.spotme.domain.model.workout.SetEntry(1, 8, 60.0, new com.spotme.domain.model.metrics.Rpe(8.0), "set")
            );
            session.finish(Instant.parse(String.format("2026-04-10T%02d:45:00Z", 9 + i)));
            session.reportRecovery(new com.spotme.domain.model.metrics.Doms(2), new com.spotme.domain.model.metrics.SleepQuality(7));
            sessions.add(session);
        }
        return Map.of(userId, sessions);
    }

    static class StubWorkoutReadPort implements WorkoutReadPort {
        final Map<UserId, List<com.spotme.domain.model.workout.WorkoutSession>> sessions;

        StubWorkoutReadPort(Map<UserId, List<com.spotme.domain.model.workout.WorkoutSession>> sessions) {
            this.sessions = sessions;
        }

        @Override
        public Optional<com.spotme.domain.rules.ProgressionInput> lastProgressionInput(
                UserId userId,
                com.spotme.domain.model.exercise.ExerciseId exerciseId) {
            return Optional.empty();
        }

        @Override
        public Optional<com.spotme.domain.model.workout.WorkoutSession> findSession(
                UserId userId,
                com.spotme.domain.model.workout.WorkoutSessionId sessionId) {
            return Optional.empty();
        }

        @Override
        public Optional<com.spotme.domain.model.workout.WorkoutSession> findLatestSession(UserId userId) {
            return sessions.getOrDefault(userId, List.of()).stream()
                    .max(java.util.Comparator.comparing(session -> session.finishedAt().orElse(session.startedAt())));
        }

        @Override
        public List<com.spotme.domain.model.workout.WorkoutSession> listSessionsFor(UserId userId, int limit) {
            return sessions.getOrDefault(userId, List.of()).stream()
                    .sorted(java.util.Comparator.comparing(session -> session.finishedAt().orElse(session.startedAt()), java.util.Comparator.reverseOrder()))
                    .limit(limit)
                    .toList();
        }
    }
}


