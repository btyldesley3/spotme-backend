package com.spotme;

import com.google.protobuf.Empty;
import com.spotme.proto.plan.v1.AuthServiceGrpc;
import com.spotme.proto.plan.v1.CompleteWorkoutSessionRequest;
import com.spotme.proto.plan.v1.GetUserProfileRequest;
import com.spotme.proto.plan.v1.LogSetRequest;
import com.spotme.proto.plan.v1.LoginRequest;
import com.spotme.proto.plan.v1.PlanServiceGrpc;
import com.spotme.proto.plan.v1.RecommendRequest;
import com.spotme.proto.plan.v1.RefreshTokenRequest;
import com.spotme.proto.plan.v1.RegisterCredentialsRequest;
import com.spotme.proto.plan.v1.StartWorkoutSessionRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "grpc.server.port=0",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
@Import(AdaptiveTrainingFlowGrpcIntegrationTest.GrpcPortCaptureConfig.class)
class AdaptiveTrainingFlowGrpcIntegrationTest {

    private static final String EXERCISE_ID = "11111111-1111-1111-1111-111111111111";
    private static final Metadata.Key<String> AUTHORIZATION_HEADER =
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    private static final EmbeddedPostgres postgres = startEmbeddedPostgres();

    @jakarta.annotation.Resource
    private AtomicInteger grpcPortHolder;

    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbc;

    private ManagedChannel channel;

    @BeforeEach
    void setUpAllowlist() {
        jdbc.update("delete from alpha_email_allowlist where email = ?", "grpc-alpha@spotme.dev");
        jdbc.update("delete from user_credentials where email = ?", "grpc-alpha@spotme.dev");
        jdbc.update(
                "insert into alpha_email_allowlist (id, email, active, notes, created_at) values (?, ?, true, ?, now())",
                UUID.randomUUID(),
                "grpc-alpha@spotme.dev",
                "grpc-integration-test"
        );
    }

    @AfterEach
    void tearDown() {
        if (channel != null) {
            channel.shutdownNow();
        }
    }

    private static EmbeddedPostgres startEmbeddedPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start embedded Postgres for integration test", e);
        }
    }

    @Test
    void userCanRegisterLogWorkoutAndReceiveAdaptiveRecommendation() {
        assertTrue(grpcPortHolder.get() > 0, "Expected gRPC server port to be captured after startup");
        channel = ManagedChannelBuilder.forAddress("localhost", grpcPortHolder.get())
                .usePlaintext()
                .build();

        var authStub = AuthServiceGrpc.newBlockingStub(channel);
        var registerResponse = authStub.registerCredentials(RegisterCredentialsRequest.newBuilder()
                .setEmail("grpc-alpha@spotme.dev")
                .setPassword("Password123!")
                .setExperienceLevel("beginner")
                .setTrainingGoal("strength")
                .setBaselineSleepHours(7)
                .setStressSensitivity(3)
                .build());
        assertNotNull(registerResponse.getUserId());

        var loginResponse = authStub.login(LoginRequest.newBuilder()
                .setEmail("grpc-alpha@spotme.dev")
                .setPassword("Password123!")
                .build());
        assertFalse(loginResponse.getAccessToken().isBlank());
        assertFalse(loginResponse.getRefreshToken().isBlank());

        var headers = new Metadata();
        headers.put(AUTHORIZATION_HEADER, "Bearer " + loginResponse.getAccessToken());
        var stub = PlanServiceGrpc.newBlockingStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));

        var profile = stub.getUserProfile(GetUserProfileRequest.newBuilder()
                .setUserId(registerResponse.getUserId())
                .build());
        assertEquals(registerResponse.getUserId(), profile.getUserId());
        assertEquals(7, profile.getBaselineSleepHours());

        var startResponse = stub.startWorkoutSession(StartWorkoutSessionRequest.newBuilder()
                .setUserId(registerResponse.getUserId())
                .setStartedAt("2026-04-16T08:00:00Z")
                .build());
        assertEquals(registerResponse.getUserId(), startResponse.getUserId());
        assertFalse(startResponse.getSessionId().isBlank());

        var logSetOne = stub.logSet(LogSetRequest.newBuilder()
                .setUserId(registerResponse.getUserId())
                .setSessionId(startResponse.getSessionId())
                .setExerciseId(EXERCISE_ID)
                .setSetNumber(1)
                .setReps(8)
                .setWeightKg(57.5)
                .setRpe(7.0)
                .setNote("first working set")
                .build());
        assertEquals(1, logSetOne.getTotalSetsInSession());

        var logSetTwo = stub.logSet(LogSetRequest.newBuilder()
                .setUserId(registerResponse.getUserId())
                .setSessionId(startResponse.getSessionId())
                .setExerciseId(EXERCISE_ID)
                .setSetNumber(2)
                .setReps(8)
                .setWeightKg(60.0)
                .setRpe(6.5)
                .setNote("top set")
                .build());
        assertEquals(2, logSetTwo.getTotalSetsInSession());

        var completeResponse = stub.completeWorkoutSession(CompleteWorkoutSessionRequest.newBuilder()
                .setUserId(registerResponse.getUserId())
                .setSessionId(startResponse.getSessionId())
                .setFinishedAt("2026-04-16T08:45:00Z")
                .setMinTotalSets(2)
                .setMinDistinctExercises(1)
                .setMinSetsPerExercise(2)
                .setRequireRecoveryFeedbackForProgression(true)
                .setDoms(2)
                .setSleepQuality(8)
                .build());

        assertTrue(completeResponse.getCompleted());
        assertTrue(completeResponse.getAllowsProgression());
        assertEquals(2, completeResponse.getTotalSets());

        var recommendation = stub.recommend(RecommendRequest.newBuilder()
                .setUserId(registerResponse.getUserId())
                .setExerciseId(EXERCISE_ID)
                .setRulesVersion("v1.0.0")
                .setModalityKey("barbell_upper")
                .build());

        assertEquals(1, recommendation.getSetsCount());
        assertEquals(EXERCISE_ID, recommendation.getSets(0).getExerciseId());
        assertEquals(8, recommendation.getSets(0).getPrescribedReps());
        assertEquals(60.75, recommendation.getSets(0).getPrescribedWeightKg());
    }

    @Test
    void authRefreshAndLogoutAreGrpcNative() {
        assertTrue(grpcPortHolder.get() > 0, "Expected gRPC server port to be captured after startup");
        channel = ManagedChannelBuilder.forAddress("localhost", grpcPortHolder.get())
                .usePlaintext()
                .build();

        var authStub = AuthServiceGrpc.newBlockingStub(channel);
        authStub.registerCredentials(RegisterCredentialsRequest.newBuilder()
                .setEmail("grpc-alpha@spotme.dev")
                .setPassword("Password123!")
                .setExperienceLevel("beginner")
                .setTrainingGoal("strength")
                .setBaselineSleepHours(7)
                .setStressSensitivity(3)
                .build());

        var loginResponse = authStub.login(LoginRequest.newBuilder()
                .setEmail("grpc-alpha@spotme.dev")
                .setPassword("Password123!")
                .build());

        var refreshResponse = authStub.refreshToken(RefreshTokenRequest.newBuilder()
                .setRefreshToken(loginResponse.getRefreshToken())
                .build());
        assertFalse(refreshResponse.getAccessToken().isBlank());
        assertFalse(refreshResponse.getRefreshToken().isBlank());

        var headers = new Metadata();
        headers.put(AUTHORIZATION_HEADER, "Bearer " + refreshResponse.getAccessToken());
        var authenticatedAuthStub = authStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
        authenticatedAuthStub.logout(Empty.getDefaultInstance());

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                StatusRuntimeException.class,
                () -> authStub.refreshToken(RefreshTokenRequest.newBuilder()
                        .setRefreshToken(refreshResponse.getRefreshToken())
                        .build())
        );
        assertEquals(Status.Code.UNAUTHENTICATED, ex.getStatus().getCode());
    }

    @Test
    void protectedGrpcMethodRejectsMissingToken() {
        assertTrue(grpcPortHolder.get() > 0, "Expected gRPC server port to be captured after startup");
        channel = ManagedChannelBuilder.forAddress("localhost", grpcPortHolder.get())
                .usePlaintext()
                .build();
        var stub = PlanServiceGrpc.newBlockingStub(channel);

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                StatusRuntimeException.class,
                () -> stub.getUserProfile(GetUserProfileRequest.newBuilder()
                        .setUserId(UUID.randomUUID().toString())
                        .build())
        );
        assertEquals(Status.Code.UNAUTHENTICATED, ex.getStatus().getCode());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GrpcPortCaptureConfig {

        @Bean
        DataSource dataSource() {
            return postgres.getPostgresDatabase();
        }

        @Bean
        AtomicInteger grpcPortHolder() {
            return new AtomicInteger();
        }

        @Bean
        ApplicationListener<GrpcServerStartedEvent> grpcPortCaptureListener(AtomicInteger grpcPortHolder) {
            return event -> grpcPortHolder.set(event.getServer().getPort());
        }
    }
}
