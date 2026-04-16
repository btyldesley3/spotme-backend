package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.WorkoutWritePort;

import java.time.Instant;

/**
 * Begins a new in-progress workout session and persists it so subsequent
 * LogSet / CompleteWorkoutSession calls can reference it by session ID.
 */
public class StartWorkoutSession {

    private final UserReadPort users;
    private final WorkoutWritePort write;

    public record Command(String userId, String startedAt) {
    }

    public record Result(WorkoutSessionId sessionId, UserId userId, Instant startedAt) {
    }

    public StartWorkoutSession(UserReadPort users, WorkoutWritePort write) {
        this.users = users;
        this.write = write;
    }

    public Result handle(Command command) {
        var userId = UserExistenceGuard.requireExistingUser(command.userId(), users);
        Instant startedAt = command.startedAt() == null || command.startedAt().isBlank()
                ? Instant.now()
                : Instant.parse(command.startedAt());

        var session = WorkoutSession.start(userId, startedAt);
        write.saveSession(session);
        return new Result(session.sessionId(), session.userId(), session.startedAt());
    }
}

