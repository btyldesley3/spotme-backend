package com.spotme.application.usecase;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.RecoveryProfile;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.spotme.domain.rules.ProgressionInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogSetTest {

    private UserId userId;
    private WorkoutSession session;
    private ExerciseId exerciseId;

    @BeforeEach
    void setup() {
        userId = UserId.random();
        session = WorkoutSession.start(userId, Instant.parse("2026-04-14T09:00:00Z"));
        exerciseId = ExerciseId.random();
    }

    @Test
    void appendsSetToInProgressSession() {
        AtomicReference<WorkoutSession> savedSession = new AtomicReference<>(session);

        WorkoutReadPort read = new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId u, ExerciseId e) { return Optional.empty(); }
            @Override
            public Optional<WorkoutSession> findSession(UserId u, WorkoutSessionId id) {
                return savedSession.get().sessionId().equals(id) ? Optional.of(savedSession.get()) : Optional.empty();
            }
            @Override
            public Optional<WorkoutSession> findLatestSession(UserId u) { return Optional.empty(); }
            @Override
            public List<WorkoutSession> listSessionsFor(UserId u, int limit) { return Collections.emptyList(); }
        };

        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId u, Prescription p) {}
            @Override public void saveSession(WorkoutSession s) { savedSession.set(s); }
        };

        var useCase = new LogSet(existingUserPort(userId), read, write);
        var result = useCase.handle(new LogSet.Command(
                userId.toString(),
                session.sessionId().toString(),
                exerciseId.toString(),
                1, 8, 60.0, 8.0, "working set"
        ));

        assertEquals(session.sessionId(), result.sessionId());
        assertEquals(1, result.totalSetsInSession());
    }

    @Test
    void setNumbersMustBeSequential() {
        WorkoutSession sessionWithSet = WorkoutSession.start(userId, Instant.parse("2026-04-14T09:00:00Z"));

        WorkoutReadPort read = new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId u, ExerciseId e) { return Optional.empty(); }
            @Override
            public Optional<WorkoutSession> findSession(UserId u, WorkoutSessionId id) {
                return Optional.of(sessionWithSet);
            }
            @Override
            public Optional<WorkoutSession> findLatestSession(UserId u) { return Optional.empty(); }
            @Override
            public List<WorkoutSession> listSessionsFor(UserId u, int limit) { return Collections.emptyList(); }
        };
        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId u, Prescription p) {}
            @Override public void saveSession(WorkoutSession s) {}
        };

        var useCase = new LogSet(existingUserPort(userId), read, write);

        // First log set 1 (ok)
        useCase.handle(new LogSet.Command(
                userId.toString(), sessionWithSet.sessionId().toString(),
                exerciseId.toString(), 1, 8, 60.0, 8.0, "first"
        ));

        // Attempting set 3 (skipping 2) should fail
        assertThrows(IllegalArgumentException.class, () -> useCase.handle(new LogSet.Command(
                userId.toString(), sessionWithSet.sessionId().toString(),
                exerciseId.toString(), 3, 8, 62.5, 8.5, "wrong order"
        )));
    }

    @Test
    void throwsNotFoundForMissingSession() {
        WorkoutReadPort read = new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId u, ExerciseId e) { return Optional.empty(); }
            @Override
            public Optional<WorkoutSession> findSession(UserId u, WorkoutSessionId id) { return Optional.empty(); }
            @Override
            public Optional<WorkoutSession> findLatestSession(UserId u) { return Optional.empty(); }
            @Override
            public List<WorkoutSession> listSessionsFor(UserId u, int limit) { return Collections.emptyList(); }
        };
        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId u, Prescription p) {}
            @Override public void saveSession(WorkoutSession s) {}
        };

        var useCase = new LogSet(existingUserPort(userId), read, write);
        assertThrows(java.util.NoSuchElementException.class, () -> useCase.handle(new LogSet.Command(
                userId.toString(), WorkoutSessionId.random().toString(),
                exerciseId.toString(), 1, 8, 60.0, 8.0, "orphan"
        )));
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        WorkoutReadPort read = new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId u, ExerciseId e) { return Optional.empty(); }
            @Override
            public Optional<WorkoutSession> findSession(UserId u, WorkoutSessionId id) { return Optional.of(session); }
            @Override
            public Optional<WorkoutSession> findLatestSession(UserId u) { return Optional.empty(); }
            @Override
            public List<WorkoutSession> listSessionsFor(UserId u, int limit) { return Collections.emptyList(); }
        };
        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId u, Prescription p) {}
            @Override public void saveSession(WorkoutSession s) {}
        };

        var useCase = new LogSet(missingUserPort(), read, write);
        assertThrows(java.util.NoSuchElementException.class, () -> useCase.handle(new LogSet.Command(
                userId.toString(), session.sessionId().toString(),
                exerciseId.toString(), 1, 8, 60.0, 8.0, "working set"
        )));
    }

    private UserReadPort existingUserPort(UserId existingUserId) {
        var user = new User(existingUserId, ExperienceLevel.BEGINNER, TrainingGoal.STRENGTH, new RecoveryProfile(7, 3));
        return requested -> requested.equals(existingUserId) ? Optional.of(user) : Optional.empty();
    }

    private UserReadPort missingUserPort() {
        return requested -> Optional.empty();
    }
}

