package com.spotme.adapters.out.persistence;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.SetEntry;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.spotme.domain.rules.ProgressionInput;

import java.time.Instant;
import java.util.*;

public class InMemoryWorkoutAdapter implements WorkoutReadPort, WorkoutWritePort {

    private final Map<UserId, List<WorkoutSession>> sessions = new HashMap<>();
    private final Map<UserId, List<Prescription>> prescriptions = new HashMap<>();

    public InMemoryWorkoutAdapter() {
        // Seed with a default completed session (bench press example)
        var userId = UserId.random();
        var exerciseId = ExerciseId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-01T10:00:00Z"));
        session.addSet(exerciseId, new SetEntry(1, 8, 57.5, new Rpe(7.5), "warm-up working set"));
        session.addSet(exerciseId, new SetEntry(2, 8, 60.0, new Rpe(8.5), "top set"));
        session.finish(Instant.parse("2026-04-01T10:45:00Z"));
        session.reportRecovery(new Doms(3), new SleepQuality(7));
        sessions.put(userId, new ArrayList<>(List.of(session)));
    }

    @Override
    public Optional<ProgressionInput> lastProgressionInput(UserId userId, ExerciseId exerciseId) {
        return sessions.getOrDefault(userId, Collections.emptyList()).stream()
                .filter(session -> session.containsExercise(exerciseId))
                .sorted(Comparator.comparing(session -> session.finishedAt().orElse(session.startedAt()), Comparator.reverseOrder()))
                .map(session -> session.progressionInputFor(exerciseId))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public Optional<WorkoutSession> findSession(UserId userId, WorkoutSessionId sessionId) {
        return sessions.getOrDefault(userId, Collections.emptyList()).stream()
                .filter(session -> session.sessionId().equals(sessionId))
                .findFirst();
    }

    @Override
    public void savePrescription(UserId userId, Prescription prescription) {
        prescriptions.computeIfAbsent(userId, k -> new ArrayList<>()).add(prescription);
    }

    @Override
    public void saveSession(WorkoutSession session) {
        var userSessions = sessions.computeIfAbsent(session.userId(), ignored -> new ArrayList<>());
        userSessions.removeIf(existing -> existing.sessionId().equals(session.sessionId()));
        userSessions.add(session);
    }

    @Override
    public Optional<WorkoutSession> findLatestSession(UserId userId) {
        return sessions.getOrDefault(userId, Collections.emptyList()).stream()
                .max(Comparator.comparing(session -> session.finishedAt().orElse(session.startedAt())));
    }

    @Override
    public List<WorkoutSession> listSessionsFor(UserId userId, int limit) {
        return sessions.getOrDefault(userId, Collections.emptyList()).stream()
                .sorted(Comparator.comparing(session -> session.finishedAt().orElse(session.startedAt()), Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }
}
