package com.spotme.adapters.in.grpc;

import com.spotme.application.usecase.GetUserProfile;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.proto.plan.v1.GetUserProfileRequest;
import com.spotme.proto.plan.v1.RegisterUserRequest;
import com.spotme.proto.plan.v1.UserProfileResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanGrpcServiceUserTest {

    @Test
    void registerUserIsBlockedForAlpha() {
        UserReadPort read = userId -> Optional.empty();

        var service = new PlanGrpcService(
                null,
                null,
                null,
                null,
                null,
                null,
                new GetUserProfile(read)
        );

        var observer = new CapturingObserver<com.spotme.proto.plan.v1.RegisterUserResponse>();
        service.registerUser(RegisterUserRequest.newBuilder()
                .setExperienceLevel("beginner")
                .setTrainingGoal("strength")
                .setBaselineSleepHours(7)
                .setStressSensitivity(3)
                .build(), observer);

        StatusRuntimeException ex = (StatusRuntimeException) observer.error;
        assertEquals(Status.Code.PERMISSION_DENIED, ex.getStatus().getCode());
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



