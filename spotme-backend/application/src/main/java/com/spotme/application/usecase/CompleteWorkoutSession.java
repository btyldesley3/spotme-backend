package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutCompletionPolicy;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.model.workout.WorkoutSessionSummary;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;

import java.time.Instant;
import java.util.UUID;

public class CompleteWorkoutSession {
    private final WorkoutReadPort read;
    private final WorkoutWritePort write;

    public record Command(
            String userId,
            String sessionId,
            String finishedAt,
            int minTotalSets,
            int minDistinctExercises,
            int minSetsPerExercise,
            boolean requireRecoveryFeedbackForProgression,
            Integer doms,
            Integer sleepQuality
    ) {
    }

    public record Result(WorkoutSessionSummary summary) {
    }

    public CompleteWorkoutSession(WorkoutReadPort read, WorkoutWritePort write) {
        this.read = read;
        this.write = write;
    }

    public Result handle(Command command) {
        var userId = new UserId(UUID.fromString(command.userId()));
        var sessionId = new WorkoutSessionId(UUID.fromString(command.sessionId()));
        var finishedAt = Instant.parse(command.finishedAt());

        var session = read.findSession(userId, sessionId).orElseThrow();
        if (!session.userId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to supplied userId");
        }

        var completionPolicy = new WorkoutCompletionPolicy(
                command.minTotalSets(),
                command.minDistinctExercises(),
                command.minSetsPerExercise(),
                command.requireRecoveryFeedbackForProgression()
        );

        session.finish(finishedAt, completionPolicy);

        boolean hasDoms = command.doms() != null;
        boolean hasSleep = command.sleepQuality() != null;
        if (hasDoms != hasSleep) {
            throw new IllegalArgumentException("doms and sleepQuality must be provided together");
        }
        if (hasDoms) {
            session.reportRecovery(new Doms(command.doms()), new SleepQuality(command.sleepQuality()));
        }

        write.saveSession(session);
        return new Result(session.summary());
    }
}

