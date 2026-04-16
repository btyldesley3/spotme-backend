package com.spotme.application.usecase;

import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.RecoveryProfile;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.port.WorkoutWritePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StartWorkoutSessionTest {

    @Test
    void createsAndPersistsNewSession() {
        AtomicReference<WorkoutSession> saved = new AtomicReference<>();
        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId userId, Prescription prescription) {}
            @Override public void saveSession(WorkoutSession session) { saved.set(session); }
        };

        var userId = UserId.random();
        var useCase = new StartWorkoutSession(existingUserPort(userId), write);
        var startedAt = "2026-04-14T10:00:00Z";

        var result = useCase.handle(new StartWorkoutSession.Command(userId.toString(), startedAt));

        assertNotNull(result.sessionId());
        assertEquals(userId, result.userId());
        assertEquals(Instant.parse(startedAt), result.startedAt());
        assertNotNull(saved.get());
        assertEquals(result.sessionId(), saved.get().sessionId());
    }

    @Test
    void usesCurrentTimeWhenStartedAtIsBlank() {
        Instant before = Instant.now();
        AtomicReference<WorkoutSession> saved = new AtomicReference<>();
        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId userId, Prescription prescription) {}
            @Override public void saveSession(WorkoutSession session) { saved.set(session); }
        };

        var userId = UserId.random();
        var result = new StartWorkoutSession(existingUserPort(userId), write).handle(
                new StartWorkoutSession.Command(userId.toString(), "")
        );

        Instant after = Instant.now();
        assertNotNull(result.startedAt());
        // Started-at should be between before/after (within the test execution window)
        assert !result.startedAt().isBefore(before) : "startedAt should not be before test began";
        assert !result.startedAt().isAfter(after) : "startedAt should not be after test finished";
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId userId, Prescription prescription) {}
            @Override public void saveSession(WorkoutSession session) {}
        };

        var useCase = new StartWorkoutSession(missingUserPort(), write);
        assertThrows(java.util.NoSuchElementException.class, () ->
                useCase.handle(new StartWorkoutSession.Command(UserId.random().toString(), "2026-04-14T10:00:00Z"))
        );
    }

    private UserReadPort existingUserPort(UserId userId) {
        var user = new User(userId, ExperienceLevel.BEGINNER, TrainingGoal.STRENGTH, new RecoveryProfile(7, 3));
        return requested -> requested.equals(userId) ? Optional.of(user) : Optional.empty();
    }

    private UserReadPort missingUserPort() {
        return requested -> Optional.empty();
    }
}

