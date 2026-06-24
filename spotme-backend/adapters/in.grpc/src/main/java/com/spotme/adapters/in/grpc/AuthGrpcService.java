package com.spotme.adapters.in.grpc;

import com.spotme.application.usecase.LoginUser;
import com.spotme.application.usecase.RegisterWithCredentials;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.RefreshTokenPort;
import com.spotme.proto.plan.v1.AuthServiceGrpc;
import com.spotme.proto.plan.v1.AuthTokensResponse;
import com.spotme.proto.plan.v1.LoginRequest;
import com.spotme.proto.plan.v1.RefreshTokenRequest;
import com.spotme.proto.plan.v1.RegisterCredentialsRequest;
import com.spotme.proto.plan.v1.RegisterCredentialsResponse;
import com.google.protobuf.Empty;
import io.grpc.Status;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final RegisterWithCredentials registerWithCredentials;
    private final LoginUser loginUser;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenPort refreshTokenPort;

    public AuthGrpcService(RegisterWithCredentials registerWithCredentials,
                           LoginUser loginUser,
                           JwtTokenService jwtTokenService,
                           RefreshTokenPort refreshTokenPort) {
        this.registerWithCredentials = registerWithCredentials;
        this.loginUser = loginUser;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenPort = refreshTokenPort;
    }

    @Override
    public void registerCredentials(RegisterCredentialsRequest request,
                                    io.grpc.stub.StreamObserver<RegisterCredentialsResponse> responseObserver) {
        try {
            var result = registerWithCredentials.handle(new RegisterWithCredentials.Command(
                    request.getEmail(),
                    request.getPassword(),
                    request.getInviteCode(),
                    request.getExperienceLevel(),
                    request.getTrainingGoal(),
                    request.getBaselineSleepHours(),
                    request.getStressSensitivity()
            ));
            responseObserver.onNext(RegisterCredentialsResponse.newBuilder()
                    .setUserId(result.userId().toString())
                    .setEmail(result.email())
                    .build());
            responseObserver.onCompleted();
        } catch (RegisterWithCredentials.AlphaAccessDeniedException e) {
            responseObserver.onError(Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void login(LoginRequest request,
                      io.grpc.stub.StreamObserver<AuthTokensResponse> responseObserver) {
        try {
            var result = loginUser.handle(new LoginUser.Command(request.getEmail(), request.getPassword()));
            responseObserver.onNext(issueTokens(result.userId(), result.email()));
            responseObserver.onCompleted();
        } catch (LoginUser.AuthenticationFailedException e) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void refreshToken(RefreshTokenRequest request,
                             io.grpc.stub.StreamObserver<AuthTokensResponse> responseObserver) {
        try {
            var tokenHash = jwtTokenService.hashToken(request.getRefreshToken());
            var userId = refreshTokenPort.validateAndConsume(tokenHash)
                    .orElseThrow(() -> new IllegalStateException("refresh token is invalid or expired"));

            responseObserver.onNext(issueTokens(userId, ""));
            responseObserver.onCompleted();
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void logout(Empty request,
                       io.grpc.stub.StreamObserver<Empty> responseObserver) {
        revokeAllForAuthenticatedUser();
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void logoutAll(Empty request,
                          io.grpc.stub.StreamObserver<Empty> responseObserver) {
        revokeAllForAuthenticatedUser();
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private AuthTokensResponse issueTokens(UserId userId, String email) {
        var accessToken = jwtTokenService.generateAccessToken(userId.toString(), email);
        var rawRefresh = jwtTokenService.generateRefreshTokenValue();
        refreshTokenPort.save(userId, jwtTokenService.hashToken(rawRefresh), jwtTokenService.refreshTokenExpiry());
        return AuthTokensResponse.newBuilder()
                .setAccessToken(accessToken)
                .setRefreshToken(rawRefresh)
                .build();
    }

    private void revokeAllForAuthenticatedUser() {
        var authenticatedUserId = GrpcAuthenticationContext.currentUserId()
                .orElseThrow(() -> Status.UNAUTHENTICATED.withDescription("Missing authenticated user context").asRuntimeException());
        refreshTokenPort.revokeAllForUser(UserId.fromString(authenticatedUserId));
    }
}

