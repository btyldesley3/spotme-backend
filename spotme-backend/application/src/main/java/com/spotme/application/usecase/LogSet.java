package com.spotme.application.usecase;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.SetEntry;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;

import java.util.UUID;

/**
 * Appends a single set to an existing in-progress workout session.
 * The session must have been created via StartWorkoutSession.
 */
public class LogSet {

    private final UserReadPort users;
    private final WorkoutReadPort read;
    private final WorkoutWritePort write;

    public record Command(
            String userId,
            String sessionId,
            String exerciseId,
            int setNumber,
            int reps,
            double weightKg,
            double rpe,
            String note
    ) {
    }

    public record Result(WorkoutSessionId sessionId, int totalSetsInSession) {
    }

    public LogSet(UserReadPort users, WorkoutReadPort read, WorkoutWritePort write) {
        this.users = users;
        this.read = read;
        this.write = write;
    }

    public Result handle(Command command) {
        var userId = UserExistenceGuard.requireExistingUser(command.userId(), users);
        var sessionId = new WorkoutSessionId(UUID.fromString(command.sessionId()));
        var exerciseId = new ExerciseId(UUID.fromString(command.exerciseId()));

        var session = read.findSession(userId, sessionId).orElseThrow();
        if (!session.userId().equals(userId)) {
            throw new IllegalArgumentException("Session does not belong to supplied userId");
        }

        session.addSet(exerciseId, new SetEntry(
                command.setNumber(),
                command.reps(),
                command.weightKg(),
                new Rpe(command.rpe()),
                command.note()
        ));

        write.saveSession(session);

        int totalSets = session.sets().values().stream()
                .mapToInt(java.util.List::size)
                .sum();

        return new Result(sessionId, totalSets);
    }
}

