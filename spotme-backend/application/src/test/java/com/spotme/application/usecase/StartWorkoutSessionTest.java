package com.spotme.application.usecase;

import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.port.WorkoutWritePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StartWorkoutSessionTest {

    @Test
    void createsAndPersistsNewSession() {
        AtomicReference<WorkoutSession> saved = new AtomicReference<>();
        WorkoutWritePort write = new WorkoutWritePort() {
            @Override public void savePrescription(UserId userId, Prescription prescription) {}
            @Override public void saveSession(WorkoutSession session) { saved.set(session); }
        };

        var useCase = new StartWorkoutSession(write);
        var userId = UserId.random();
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

        var result = new StartWorkoutSession(write).handle(
                new StartWorkoutSession.Command(UserId.random().toString(), "")
        );

        Instant after = Instant.now();
        assertNotNull(result.startedAt());
        // Started-at should be between before/after (within the test execution window)
        assert !result.startedAt().isBefore(before) : "startedAt should not be before test began";
        assert !result.startedAt().isAfter(after) : "startedAt should not be after test finished";
    }
}

