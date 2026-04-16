package com.spotme.application.usecase;

import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.RecoveryProfile;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserWritePort;

import java.util.Locale;

public class RegisterUser {

    private final UserWritePort write;

    public record Command(
            String experienceLevel,
            String trainingGoal,
            int baselineSleepHours,
            int stressSensitivity
    ) {}

    public record Result(
            UserId userId,
            ExperienceLevel experienceLevel,
            TrainingGoal trainingGoal,
            int baselineSleepHours,
            int stressSensitivity
    ) {}

    public RegisterUser(UserWritePort write) {
        this.write = write;
    }

    public Result handle(Command command) {
        validateRecoveryProfile(command.baselineSleepHours(), command.stressSensitivity());

        var user = new User(
                UserId.random(),
                parseExperienceLevel(command.experienceLevel()),
                parseTrainingGoal(command.trainingGoal()),
                new RecoveryProfile(command.baselineSleepHours(), command.stressSensitivity())
        );
        write.save(user);

        return new Result(
                user.id(),
                user.experienceLevel(),
                user.trainingGoal(),
                user.recoveryProfile().baselineSleepHours(),
                user.recoveryProfile().stressSensitivity()
        );
    }

    private ExperienceLevel parseExperienceLevel(String value) {
        return ExperienceLevel.valueOf(requireNonBlank(value, "experienceLevel").toUpperCase(Locale.ROOT));
    }

    private TrainingGoal parseTrainingGoal(String value) {
        return TrainingGoal.valueOf(requireNonBlank(value, "trainingGoal").toUpperCase(Locale.ROOT));
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private static void validateRecoveryProfile(int baselineSleepHours, int stressSensitivity) {
        if (baselineSleepHours < 1 || baselineSleepHours > 24) {
            throw new IllegalArgumentException("baselineSleepHours must be between 1 and 24");
        }
        if (stressSensitivity < 1 || stressSensitivity > 5) {
            throw new IllegalArgumentException("stressSensitivity must be between 1 and 5");
        }
    }
}

