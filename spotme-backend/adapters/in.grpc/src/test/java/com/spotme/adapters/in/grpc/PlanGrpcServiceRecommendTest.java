package com.spotme.adapters.in.grpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotme.application.usecase.ComputeNextPrescription;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.RulesConfigPort;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.spotme.domain.rules.ProgressionInput;
import com.spotme.proto.plan.v1.RecommendRequest;
import com.spotme.proto.plan.v1.RecommendResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanGrpcServiceRecommendTest {

    @Test
    void recommendReturnsPrescriptionForKnownHistory() throws Exception {
        UserId userId = UserId.random();
        ExerciseId exerciseId = ExerciseId.random();

        WorkoutReadPort read = new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId requestedUserId, ExerciseId requestedExerciseId) {
                if (!requestedUserId.equals(userId) || !requestedExerciseId.equals(exerciseId)) {
                    return Optional.empty();
                }
                return Optional.of(new ProgressionInput(60.0, 8, new Rpe(6.5), new Doms(0), new SleepQuality(9)));
            }

            @Override
            public Optional<WorkoutSession> findSession(UserId userId, WorkoutSessionId sessionId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkoutSession> findLatestSession(UserId userId) {
                return Optional.empty();
            }

            @Override
            public java.util.List<WorkoutSession> listSessionsFor(UserId userId, int limit) {
                return Collections.emptyList();
            }
        };

        WorkoutWritePort write = new WorkoutWritePort() {
            @Override
            public void savePrescription(UserId userId, Prescription prescription) {
                // No-op for endpoint mapping test.
            }

            @Override
            public void saveSession(WorkoutSession session) {
                // No-op for endpoint mapping test.
            }
        };

        RulesConfigPort rules = version -> rulesJson();
        var useCase = new ComputeNextPrescription(read, write, rules);
        var service = new PlanGrpcService(useCase, null, null, null, null, null);

        var observer = new CapturingObserver<RecommendResponse>();
        service.recommend(RecommendRequest.newBuilder()
                .setUserId(userId.toString())
                .setExerciseId(exerciseId.toString())
                .setRulesVersion("v1.0.0")
                .setModalityKey("barbell_upper")
                .build(), observer);

        assertNull(observer.error);
        assertTrue(observer.completed);
        assertNotNull(observer.value);
        assertEquals(1, observer.value.getSetsCount());
        assertEquals(61.25, observer.value.getSets(0).getPrescribedWeightKg());
    }

    @Test
    void recommendMapsMissingHistoryToNotFound() {
        WorkoutReadPort read = new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId requestedUserId, ExerciseId requestedExerciseId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkoutSession> findSession(UserId userId, WorkoutSessionId sessionId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkoutSession> findLatestSession(UserId userId) {
                return Optional.empty();
            }

            @Override
            public java.util.List<WorkoutSession> listSessionsFor(UserId userId, int limit) {
                return Collections.emptyList();
            }
        };

        WorkoutWritePort write = new WorkoutWritePort() {
            @Override
            public void savePrescription(UserId userId, Prescription prescription) {
                // No-op
            }

            @Override
            public void saveSession(WorkoutSession session) {
                // No-op
            }
        };

        RulesConfigPort rules = version -> rulesJson();

        var service = new PlanGrpcService(new ComputeNextPrescription(read, write, rules), null, null, null, null, null);
        var observer = new CapturingObserver<RecommendResponse>();

        service.recommend(RecommendRequest.newBuilder()
                .setUserId(UUID.randomUUID().toString())
                .setExerciseId(UUID.randomUUID().toString())
                .build(), observer);

        StatusRuntimeException ex = (StatusRuntimeException) observer.error;
        assertEquals(Status.Code.NOT_FOUND, ex.getStatus().getCode());
    }

    @Test
    void recommendMapsInvalidUuidToInvalidArgument() {
        WorkoutReadPort read = new WorkoutReadPort() {
            @Override
            public Optional<ProgressionInput> lastProgressionInput(UserId requestedUserId, ExerciseId requestedExerciseId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkoutSession> findSession(UserId userId, WorkoutSessionId sessionId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkoutSession> findLatestSession(UserId userId) {
                return Optional.empty();
            }

            @Override
            public java.util.List<WorkoutSession> listSessionsFor(UserId userId, int limit) {
                return Collections.emptyList();
            }
        };

        WorkoutWritePort write = new WorkoutWritePort() {
            @Override
            public void savePrescription(UserId userId, Prescription prescription) {
                // No-op
            }

            @Override
            public void saveSession(WorkoutSession session) {
                // No-op
            }
        };

        RulesConfigPort rules = version -> {
            throw new IllegalArgumentException("unknown rules version");
        };

        var service = new PlanGrpcService(new ComputeNextPrescription(read, write, rules), null, null, null, null, null);
        var observer = new CapturingObserver<RecommendResponse>();

        service.recommend(RecommendRequest.newBuilder()
                .setUserId("not-a-uuid")
                .setExerciseId(UUID.randomUUID().toString())
                .build(), observer);

        StatusRuntimeException ex = (StatusRuntimeException) observer.error;
        assertEquals(Status.Code.INVALID_ARGUMENT, ex.getStatus().getCode());
    }

    private JsonNode rulesJson() {
        try {
            return new ObjectMapper().readTree("""
                    {
                      "progression_logic": {
                        "rpe_thresholds": {
                          "low": 7.0,
                          "optimal_min": 8.0,
                          "optimal_max": 9.0,
                          "high": 9.5
                        },
                        "micro_loading_policy": {
                          "barbell_upper": {
                            "min_inc": 1.25
                          }
                        }
                      },
                      "recovery": {
                        "doms": {
                          "severe_threshold": 7,
                          "moderate_threshold": 4
                        },
                        "sleep": {
                          "poor_threshold": 4
                        },
                        "weighting": {
                          "doms_impact": 0.5,
                          "rpe_influence": 0.3,
                          "sleep_influence": 0.2
                        },
                        "safety": {
                          "min_load_reduction_pct": 2.5
                        }
                      }
                    }
                    """);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build rules JSON for test", e);
        }
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


