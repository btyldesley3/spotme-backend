package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSessionSummary;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.WorkoutReadPort;

import java.util.UUID;

public class GetLatestWorkoutSession {
    private final UserReadPort users;
    private final WorkoutReadPort read;

    public record Command(String userId) {
    }

    public record Result(WorkoutSessionSummary summary) {
    }

    public GetLatestWorkoutSession(UserReadPort users, WorkoutReadPort read) {
        this.users = users;
        this.read = read;
    }

    public Result handle(Command command) {
        var userId = UserExistenceGuard.requireExistingUser(command.userId(), users);
        var session = read.findLatestSession(userId).orElseThrow();
        return new Result(session.summary());
    }
}

