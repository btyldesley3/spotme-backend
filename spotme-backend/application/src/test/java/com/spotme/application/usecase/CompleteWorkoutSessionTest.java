package com.spotme.application.usecase;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.SetEntry;
import com.spotme.domain.model.workout.WorkoutCompletionDecision;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.spotme.domain.rules.ProgressionInput;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompleteWorkoutSessionTest {

    @Test
    void completesSessionPersistsItAndReturnsSummary() {
        var userId = UserId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-13T10:00:00Z"));
        var exerciseId = ExerciseId.random();
        session.addSet(exerciseId, new SetEntry(1, 8, 60.0, new Rpe(8.0), "set-1"));

        WorkoutReadPort read = mapBackedReadPort(Map.of(session.sessionId(), session), userId, exerciseId);
        AtomicReference<WorkoutSession> saved = new AtomicReference<>();
        WorkoutWritePort write = captureWritePort(saved);

        var useCase = new CompleteWorkoutSession(read, write);
        var result = useCase.handle(new CompleteWorkoutSession.Command(
                userId.toString(),
                session.sessionId().toString(),
                "2026-04-13T10:45:00Z",
                1,
                1,
                1,
                true,
                2,
                7
        ));

        assertTrue(result.summary().completionDecision().completed());
        assertTrue(result.summary().completionDecision().allowsProgression());
        assertEquals(WorkoutCompletionDecision.Reason.COMPLETED, result.summary().completionDecision().reason());
        assertEquals(1, result.summary().totalSets());
        assertEquals(session.sessionId(), saved.get().sessionId());
        assertTrue(saved.get().finishedAt().isPresent());
    }

    @Test
    void marksSessionCompletedButNotProgressionEligibleWhenRecoveryMissingAndRequired() {
        var userId = UserId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-13T11:00:00Z"));
        var exerciseId = ExerciseId.random();
        session.addSet(exerciseId, new SetEntry(1, 8, 60.0, new Rpe(8.0), "set-1"));

        WorkoutReadPort read = mapBackedReadPort(Map.of(session.sessionId(), session), userId, exerciseId);
        AtomicReference<WorkoutSession> saved = new AtomicReference<>();
        WorkoutWritePort write = captureWritePort(saved);

        var useCase = new CompleteWorkoutSession(read, write);
        var result = useCase.handle(new CompleteWorkoutSession.Command(
                userId.toString(),
                session.sessionId().toString(),
                "2026-04-13T11:30:00Z",
                1,
                1,
                1,
                true,
                null,
                null
        ));

        assertTrue(result.summary().completionDecision().completed());
        assertFalse(result.summary().completionDecision().allowsProgression());
        assertEquals(WorkoutCompletionDecision.Reason.RECOVERY_FEEDBACK_MISSING, result.summary().completionDecision().reason());
        assertEquals(session.sessionId(), saved.get().sessionId());
    }

    @Test
    void rejectsPartialRecoveryPayload() {
        var userId = UserId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-13T12:00:00Z"));
        var exerciseId = ExerciseId.random();
        session.addSet(exerciseId, new SetEntry(1, 8, 60.0, new Rpe(8.0), "set-1"));

        WorkoutReadPort read = mapBackedReadPort(Map.of(session.sessionId(), session), userId, exerciseId);
        WorkoutWritePort write = captureWritePort(new AtomicReference<>());

        var useCase = new CompleteWorkoutSession(read, write);

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(new CompleteWorkoutSession.Command(
                userId.toString(),
                session.sessionId().toString(),
                "2026-04-13T12:30:00Z",
                1,
                1,
                1,
                true,
                3,
                null
        )));
    }

    private WorkoutReadPort mapBackedReadPort(Map<WorkoutSessionId, WorkoutSession> sessions,
                                              UserId expectedUserId,
                                              ExerciseId expectedExerciseId) {
        return new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId userId, ExerciseId exerciseId) {
                if (!userId.equals(expectedUserId) || !exerciseId.equals(expectedExerciseId)) {
                    return Optional.empty();
                }
                return sessions.values().stream().findFirst().flatMap(session -> session.progressionInputFor(exerciseId));
            }

            @Override
            public Optional<WorkoutSession> findSession(UserId userId, WorkoutSessionId sessionId) {
                if (!userId.equals(expectedUserId)) {
                    return Optional.empty();
                }
                return Optional.ofNullable(sessions.get(sessionId));
            }

            @Override
            public Optional<WorkoutSession> findLatestSession(UserId userId) {
                return Optional.empty();
            }

            @Override
            public java.util.List<WorkoutSession> listSessionsFor(UserId userId, int limit) {
                return java.util.Collections.emptyList();
            }
        };
    }

    private WorkoutWritePort captureWritePort(AtomicReference<WorkoutSession> savedSessionRef) {
        return new WorkoutWritePort() {
            @Override
            public void savePrescription(UserId userId, Prescription prescription) {
                // Not used in this use-case.
            }

            @Override
            public void saveSession(WorkoutSession session) {
                savedSessionRef.set(session);
            }
        };
    }
}

