package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.WorkoutReadPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ListRecentWorkoutSessionsTest {

    @Test
    void limitsResultsToRequestedCount() {
        var userId = UserId.random();
        var workoutPort = createMockWorkoutPort(userId, 5);

        var useCase = new ListRecentWorkoutSessions(existingUserPort(userId), workoutPort);
        var result = useCase.handle(new ListRecentWorkoutSessions.Command(userId.toString(), 3));

        assertEquals(3, result.sessions().size());
    }

    @Test
    void enforceMaxLimitBound() {
        var userId = UserId.random();
        var workoutPort = createMockWorkoutPort(userId, 15);

        var useCase = new ListRecentWorkoutSessions(existingUserPort(userId), workoutPort);
        var result = useCase.handle(new ListRecentWorkoutSessions.Command(userId.toString(), 150));

        // 150 > MAX_LIMIT(100), but only 15 sessions exist in storage.
        assertEquals(15, result.sessions().size());
    }

    @Test
    void returnsEmptyWhenNoSessionsExist() {
        var userId = UserId.random();
        var workoutPort = new StubWorkoutReadPort(Map.of());

        var useCase = new ListRecentWorkoutSessions(existingUserPort(userId), workoutPort);
        var result = useCase.handle(new ListRecentWorkoutSessions.Command(userId.toString(), 10));

        assertEquals(0, result.sessions().size());
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        var userId = UserId.random();
        var workoutPort = createMockWorkoutPort(userId, 3);

        var useCase = new ListRecentWorkoutSessions(missingUserPort(), workoutPort);

        assertThrows(java.util.NoSuchElementException.class, () ->
                useCase.handle(new ListRecentWorkoutSessions.Command(userId.toString(), 3))
        );
    }

    private UserReadPort existingUserPort(UserId userId) {
        var user = new com.spotme.domain.model.user.User(
                userId,
                com.spotme.domain.model.user.ExperienceLevel.BEGINNER,
                com.spotme.domain.model.user.TrainingGoal.STRENGTH,
                new com.spotme.domain.model.user.RecoveryProfile(7, 3)
        );
        return requested -> requested.equals(userId) ? Optional.of(user) : Optional.empty();
    }

    private UserReadPort missingUserPort() {
        return requested -> Optional.empty();
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

