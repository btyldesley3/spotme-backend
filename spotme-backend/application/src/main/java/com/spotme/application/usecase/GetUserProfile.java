package com.spotme.application.usecase;

import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;

import java.util.UUID;

public class GetUserProfile {

    private final UserReadPort read;

    public record Command(String userId) {}

    public record Result(
            UserId userId,
            ExperienceLevel experienceLevel,
            TrainingGoal trainingGoal,
            int baselineSleepHours,
            int stressSensitivity
    ) {}

    public GetUserProfile(UserReadPort read) {
        this.read = read;
    }

    public Result handle(Command command) {
        var userId = new UserId(UUID.fromString(command.userId()));
        var user = read.findById(userId).orElseThrow();
        return new Result(
                user.id(),
                user.experienceLevel(),
                user.trainingGoal(),
                user.recoveryProfile().baselineSleepHours(),
                user.recoveryProfile().stressSensitivity()
        );
    }
}

