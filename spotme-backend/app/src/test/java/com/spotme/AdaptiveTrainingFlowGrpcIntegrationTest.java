package com.spotme;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import com.spotme.proto.plan.v1.CompleteWorkoutSessionRequest;
import com.spotme.proto.plan.v1.GetUserProfileRequest;
import com.spotme.proto.plan.v1.LogSetRequest;
import com.spotme.proto.plan.v1.PlanServiceGrpc;
import com.spotme.proto.plan.v1.RecommendRequest;
import com.spotme.proto.plan.v1.RegisterUserRequest;
import com.spotme.proto.plan.v1.StartWorkoutSessionRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "grpc.server.port=0",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
@Import(AdaptiveTrainingFlowGrpcIntegrationTest.GrpcPortCaptureConfig.class)
class AdaptiveTrainingFlowGrpcIntegrationTest {

    private static final String EXERCISE_ID = "11111111-1111-1111-1111-111111111111";

    private static final EmbeddedPostgres postgres = startEmbeddedPostgres();

    @jakarta.annotation.Resource
    private AtomicInteger grpcPortHolder;

    private ManagedChannel channel;

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
        var stub = PlanServiceGrpc.newBlockingStub(channel);

        var registerResponse = stub.registerUser(RegisterUserRequest.newBuilder()
                .setExperienceLevel("beginner")
                .setTrainingGoal("strength")
                .setBaselineSleepHours(7)
                .setStressSensitivity(3)
                .build());

        assertFalse(registerResponse.getUserId().isBlank());
        assertEquals("BEGINNER", registerResponse.getExperienceLevel());
        assertEquals("STRENGTH", registerResponse.getTrainingGoal());

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
        assertFalse(completeResponse.getSessionId().isBlank());

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







