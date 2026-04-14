package com.spotme.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.SetEntry;
import com.spotme.domain.model.workout.WorkoutSession;
import com.spotme.domain.model.workout.WorkoutSessionId;
import com.spotme.domain.port.RulesConfigPort;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComputeNextPrescriptionTest {

    @Test
    void computesPrescriptionFromWorkoutHistoryAndPersistsIt() throws Exception {
        var userId = UserId.random();
        var exerciseId = ExerciseId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-10T09:00:00Z"));
        session.addSet(exerciseId, new SetEntry(1, 8, 57.5, new Rpe(7.0), "working set"));
        session.addSet(exerciseId, new SetEntry(2, 8, 60.0, new Rpe(6.5), "top set"));
        session.finish(Instant.parse("2026-04-10T09:45:00Z"));
        session.reportRecovery(new Doms(0), new SleepQuality(9));

        WorkoutReadPort readPort = new WorkoutReadPort() {
            @Override
            public Optional<com.spotme.domain.rules.ProgressionInput> lastProgressionInput(UserId requestedUserId, ExerciseId requestedExerciseId) {
                if (!requestedUserId.equals(userId) || !requestedExerciseId.equals(exerciseId)) {
                    return Optional.empty();
                }
                return session.progressionInputFor(requestedExerciseId);
            }

            @Override
            public Optional<WorkoutSession> findSession(UserId requestedUserId, WorkoutSessionId sessionId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkoutSession> findLatestSession(UserId userId) {
                return Optional.empty();
            }

            @Override
            public java.util.List<WorkoutSession> listSessionsFor(UserId userId, int limit) {
                return java.util.Collections.emptyList();
            }
        };

        AtomicReference<Prescription> savedPrescription = new AtomicReference<>();
        WorkoutWritePort writePort = new WorkoutWritePort() {
            @Override
            public void savePrescription(UserId requestedUserId, Prescription prescription) {
                assertEquals(userId, requestedUserId);
                savedPrescription.set(prescription);
            }

            @Override
            public void saveSession(WorkoutSession sessionToSave) {
                // Not used in this use-case test.
            }
        };

        var rulesJson = new ObjectMapper().readTree("""
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
        RulesConfigPort rulesPort = version -> rulesJson;

        var useCase = new ComputeNextPrescription(readPort, writePort, rulesPort);
        var result = useCase.handle(new ComputeNextPrescription.Command(
                userId.toString(),
                exerciseId.toString(),
                "v-test",
                "barbell_upper"
        ));

        var set = result.prescription().sets().getFirst();
        assertEquals(61.25, set.prescribedWeightKg());
        assertEquals(8, set.prescribedReps());
        assertEquals(result.prescription(), savedPrescription.get());
    }

    @Test
    void forwardsRulesVersionToRulesConfigPort() throws Exception {
        var userId = UserId.random();
        var exerciseId = ExerciseId.random();
        var session = WorkoutSession.start(userId, Instant.parse("2026-04-10T09:00:00Z"));
        session.addSet(exerciseId, new SetEntry(1, 8, 57.5, new Rpe(7.0), "working set"));
        session.addSet(exerciseId, new SetEntry(2, 8, 60.0, new Rpe(6.5), "top set"));
        session.finish(Instant.parse("2026-04-10T09:45:00Z"));
        session.reportRecovery(new Doms(0), new SleepQuality(9));

        WorkoutReadPort readPort = new WorkoutReadPort() {
            @Override
            public Optional<com.spotme.domain.rules.ProgressionInput> lastProgressionInput(UserId requestedUserId, ExerciseId requestedExerciseId) {
                if (!requestedUserId.equals(userId) || !requestedExerciseId.equals(exerciseId)) {
                    return Optional.empty();
                }
                return session.progressionInputFor(requestedExerciseId);
            }

            @Override
            public Optional<WorkoutSession> findSession(UserId requestedUserId, WorkoutSessionId sessionId) {
                return Optional.empty();
            }

            @Override
            public Optional<WorkoutSession> findLatestSession(UserId requestedUserId) {
                return Optional.empty();
            }

            @Override
            public java.util.List<WorkoutSession> listSessionsFor(UserId requestedUserId, int limit) {
                return java.util.Collections.emptyList();
            }
        };

        WorkoutWritePort writePort = new WorkoutWritePort() {
            @Override
            public void savePrescription(UserId requestedUserId, Prescription prescription) {
                // No-op for this forwarding test.
            }

            @Override
            public void saveSession(WorkoutSession sessionToSave) {
                // No-op for this forwarding test.
            }
        };

        AtomicReference<String> capturedVersion = new AtomicReference<>();
        RulesConfigPort rulesPort = version -> {
            capturedVersion.set(version);
            if (!"v1.0.0".equals(version)) {
                throw new IllegalArgumentException("unknown rules version");
            }
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
                throw new IllegalStateException(e);
            }
        };

        var useCase = new ComputeNextPrescription(readPort, writePort, rulesPort);
        useCase.handle(new ComputeNextPrescription.Command(
                userId.toString(),
                exerciseId.toString(),
                "v1.0.0",
                "barbell_upper"
        ));

        assertEquals("v1.0.0", capturedVersion.get());

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(new ComputeNextPrescription.Command(
                userId.toString(),
                exerciseId.toString(),
                "v2.0.0",
                "barbell_upper"
        )));
    }
}



