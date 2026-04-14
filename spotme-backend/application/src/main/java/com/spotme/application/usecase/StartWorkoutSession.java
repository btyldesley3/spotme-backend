package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.WorkoutWritePort;

import java.time.Instant;
import java.util.UUID;

/**
 * Begins a new in-progress workout session and persists it so subsequent
 * LogSet / CompleteWorkoutSession calls can reference it by session ID.
 */
public class StartWorkoutSession {

    private final WorkoutWritePort write;

    public record Command(String userId, String startedAt) {
    }

    public record Result(WorkoutSessionId sessionId, UserId userId, Instant startedAt) {
    }

    public StartWorkoutSession(WorkoutWritePort write) {
        this.write = write;
    }

    public Result handle(Command command) {
        var userId = new UserId(UUID.fromString(command.userId()));
        Instant startedAt = command.startedAt() == null || command.startedAt().isBlank()
                ? Instant.now()
                : Instant.parse(command.startedAt());

        var session = WorkoutSession.start(userId, startedAt);
        write.saveSession(session);
        return new Result(session.sessionId(), session.userId(), session.startedAt());
    }
}

