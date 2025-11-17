package com.spotme.domain.model.workout;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate root representing a single completed or in-progress workout session.
 */
public class WorkoutSession {

    private final WorkoutSessionId sessionId;
    private final UUID userId;
    private final Instant startedAt;
    private Instant finishedAt;

    private final Map<ExerciseId, List<SetEntry>> setsByExercise = new LinkedHashMap<>();
    private Doms domsReport;

    public WorkoutSession(WorkoutSessionId sessionId, UUID userId, Instant startedAt) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(startedAt, "startedAt");
        this.sessionId = sessionId;
        this.userId = userId;
        this.startedAt = startedAt;
    }

    public void addSet(ExerciseId exId, SetEntry set) {
        setsByExercise.computeIfAbsent(exId, k -> new ArrayList<>()).add(set);
    }

    public void finish(Instant when) {
        this.finishedAt = when;
    }

    public void reportDoms(Doms doms) {
        this.domsReport = doms;
    }

    // Accessors

    public WorkoutSessionId sessionId() {
        return sessionId;
    }

    public UUID userId() {
        return userId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<Doms> doms() {
        return Optional.ofNullable(domsReport);
    }

    public Map<ExerciseId, List<SetEntry>> sets() {
        return Collections.unmodifiableMap(setsByExercise);
    }
}
