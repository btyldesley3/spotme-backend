package com.spotme.application.usecase;

import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.RecoveryProfile;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetUserProfileTest {

    @Test
    void returnsUserProfileForKnownUser() {
        var userId = UserId.random();
        var user = new User(userId, ExperienceLevel.INTERMEDIATE, TrainingGoal.HYPERTROPHY, new RecoveryProfile(8, 2));
        UserReadPort read = requested -> requested.equals(userId) ? Optional.of(user) : Optional.empty();

        var result = new GetUserProfile(read).handle(new GetUserProfile.Command(userId.toString()));

        assertEquals(userId, result.userId());
        assertEquals(ExperienceLevel.INTERMEDIATE, result.experienceLevel());
        assertEquals(TrainingGoal.HYPERTROPHY, result.trainingGoal());
        assertEquals(8, result.baselineSleepHours());
        assertEquals(2, result.stressSensitivity());
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        UserReadPort read = requested -> Optional.empty();

        assertThrows(java.util.NoSuchElementException.class, () ->
                new GetUserProfile(read).handle(new GetUserProfile.Command(UserId.random().toString()))
        );
    }
}

