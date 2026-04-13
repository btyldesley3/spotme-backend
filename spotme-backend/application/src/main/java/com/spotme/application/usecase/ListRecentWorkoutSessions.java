package com.spotme.application.usecase;

import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSessionSummary;
import com.spotme.domain.port.WorkoutReadPort;

import java.util.List;
import java.util.UUID;

public class ListRecentWorkoutSessions {
    private static final int MAX_LIMIT = 100;
    private final WorkoutReadPort read;

    public record Command(String userId, int limit) {
    }

    public record Result(List<WorkoutSessionSummary> sessions) {
    }

    public ListRecentWorkoutSessions(WorkoutReadPort read) {
        this.read = read;
    }

    public Result handle(Command command) {
        var userId = new UserId(UUID.fromString(command.userId()));
        int safeLimit = Math.min(command.limit(), MAX_LIMIT);
        if (safeLimit < 1) {
            safeLimit = 10;
        }
        var sessions = read.listSessionsFor(userId, safeLimit).stream()
                .map(WorkoutSession::summary)
                .toList();
        return new Result(sessions);
    }
}


