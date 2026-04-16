package com.spotme.adapters.in.grpc;

import com.spotme.application.usecase.GetUserProfile;
import com.spotme.application.usecase.RegisterUser;
import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.UserWritePort;
import com.spotme.proto.plan.v1.GetUserProfileRequest;
import com.spotme.proto.plan.v1.RegisterUserRequest;
import com.spotme.proto.plan.v1.RegisterUserResponse;
import com.spotme.proto.plan.v1.UserProfileResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanGrpcServiceUserTest {

    @Test
    void registerUserReturnsCreatedProfile() {
        var users = new ConcurrentHashMap<UserId, User>();
        UserWritePort write = user -> users.put(user.id(), user);
        UserReadPort read = userId -> Optional.ofNullable(users.get(userId));

        var service = new PlanGrpcService(
                null,
                null,
                null,
                null,
                null,
                null,
                new RegisterUser(write),
                new GetUserProfile(read)
        );

        var observer = new CapturingObserver<RegisterUserResponse>();
        service.registerUser(RegisterUserRequest.newBuilder()
                .setExperienceLevel("beginner")
                .setTrainingGoal("strength")
                .setBaselineSleepHours(7)
                .setStressSensitivity(3)
                .build(), observer);

        assertNull(observer.error);
        assertTrue(observer.completed);
        assertNotNull(observer.value);
        assertEquals("BEGINNER", observer.value.getExperienceLevel());
        assertEquals("STRENGTH", observer.value.getTrainingGoal());
    }

    @Test
    void getUserProfileMapsMissingUserToNotFound() {
        UserReadPort read = userId -> Optional.empty();

        var service = new PlanGrpcService(
                null,
                null,
                null,
                null,
                null,
                null,
                new RegisterUser(user -> { }),
                new GetUserProfile(read)
        );

        var observer = new CapturingObserver<UserProfileResponse>();
        service.getUserProfile(GetUserProfileRequest.newBuilder().setUserId(UserId.random().toString()).build(), observer);

        StatusRuntimeException ex = (StatusRuntimeException) observer.error;
        assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    }

    private static final class CapturingObserver<T> implements StreamObserver<T> {
        private T value;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }
}



