package com.spotme.application.usecase;

import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.User;
import com.spotme.domain.port.UserWritePort;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegisterUserTest {

    @Test
    void createsAndPersistsNewUser() {
        AtomicReference<User> saved = new AtomicReference<>();
        UserWritePort write = saved::set;

        var result = new RegisterUser(write).handle(new RegisterUser.Command(
                "beginner",
                "strength",
                7,
                3
        ));

        assertNotNull(result.userId());
        assertEquals(ExperienceLevel.BEGINNER, result.experienceLevel());
        assertEquals(TrainingGoal.STRENGTH, result.trainingGoal());
        assertEquals(7, result.baselineSleepHours());
        assertEquals(3, result.stressSensitivity());
        assertEquals(result.userId(), saved.get().id());
    }

    @Test
    void rejectsOutOfRangeRecoveryProfile() {
        UserWritePort write = user -> { };

        assertThrows(IllegalArgumentException.class, () ->
                new RegisterUser(write).handle(new RegisterUser.Command("beginner", "strength", 0, 3))
        );

        assertThrows(IllegalArgumentException.class, () ->
                new RegisterUser(write).handle(new RegisterUser.Command("beginner", "strength", 7, 6))
        );
    }
}

