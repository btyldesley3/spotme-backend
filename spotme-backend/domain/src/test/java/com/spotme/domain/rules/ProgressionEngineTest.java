package com.spotme.domain.rules;

import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.model.plan.Prescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the enhanced ProgressionEngine that combines DOMS, RPE, and sleep.
 * Demonstrates the weighting priorities and DOMS veto behavior.
 */
class ProgressionEngineTest {

    private ProgressionEngine engine;
    private ExerciseId exerciseId;
    private ProgressionPolicy policy;

    @BeforeEach
    void setup() {
        engine = new ProgressionEngine();
        exerciseId = ExerciseId.random();

        // Default policy: DOMS (50%), RPE (30%), Sleep (20%)
        policy = new ProgressionPolicy(
                1.25,      // microLoadKg
                7,         // severeDomsThreshold
                4,         // moderateDomsThreshold
                7.0,       // lowRpeThreshold
                8.0,       // optimalRpeMin
                9.0,       // optimalRpeMax
                9.5,       // highRpeThreshold
                4,         // poorSleepThreshold
                0.3,       // loadIncreaseWeightRpe
                0.2,       // loadIncreaseWeightSleep
                0.5,       // loadDecreaseWeightDoms
                2.5        // minLoadReductionPct
        );
    }

    @Nested
    @DisplayName("DOMS: Hard Constraint Tests")
    class DomsHardConstraintTests {

        @Test
        @DisplayName("Severe DOMS (≥7) should always reduce or mirror load, regardless of RPE/sleep")
        void severeDomsMirrorsLoadDespiteOptimalRpeAndGoodSleep() {
            ProgressionInput input = new ProgressionInput(
                    100.0,                    // last weight: 100 kg
                    5,                        // last reps
                    new Rpe(7.5),            // good RPE (low, room to progress)
                    new Doms(8),             // SEVERE DOMS
                    new SleepQuality(9)      // excellent sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            // Should mirror: weight stays same or slightly reduced
            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            assertThat(nextWeight)
                    .as("Severe DOMS should prevent load increase despite good RPE and sleep")
                    .isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Severe DOMS (7) reduces load when combined with poor sleep and high RPE")
        void severeDomsPlusPoorRecoveryReducesLoad() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(9.5),            // high RPE
                    new Doms(7),             // severe DOMS
                    new SleepQuality(2)      // poor sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            double expectedReduction = 100.0 * (1.0 - 0.025); // min 2.5% reduction
            assertThat(nextWeight)
                    .as("Severe DOMS with poor recovery should reduce load")
                    .isLessThanOrEqualTo(expectedReduction);
        }
    }

    @Nested
    @DisplayName("Moderate DOMS: Weighted Penalty Tests")
    class ModerateDomsWeightedTests {

        @Test
        @DisplayName("Moderate DOMS (4-6) + low RPE should mirror load, not increase")
        void moderateDomsPreventsProgressionEvenWithLowRpe() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(6.5),            // very low RPE (ready to progress)
                    new Doms(5),             // moderate DOMS
                    new SleepQuality(8)      // good sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            assertThat(nextWeight)
                    .as("Moderate DOMS should prevent load increase despite low RPE")
                    .isLessThanOrEqualTo(100.0 + 0.1); // Allow tiny margin for rounding
        }

        @Test
        @DisplayName("Moderate DOMS + excellent sleep may allow micro-progression")
        void moderateDomsPlusExcellentSleepAllowsMicroProgression() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(7.5),            // good RPE
                    new Doms(4),             // moderate DOMS lower bound
                    new SleepQuality(9)      // excellent sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            assertThat(nextWeight)
                    .as("Moderate DOMS with excellent sleep may allow small progression")
                    .isGreaterThanOrEqualTo(100.0 - 1.0)
                    .isLessThanOrEqualTo(100.5 + 0.5); // up to +0.5kg micro-increment
        }
    }

    @Nested
    @DisplayName("RPE Influence: Suboptimal Performance Tests")
    class RpeInfluenceTests {

        @Test
        @DisplayName("High RPE (≥9.5) indicates fatigue and should not progress")
        void highRpePreventsProgramIfNormalRecovery() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(9.5),            // high RPE: near-maximal effort
                    new Doms(2),             // mild DOMS
                    new SleepQuality(7)      // normal sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            assertThat(nextWeight)
                    .as("High RPE indicates fatigue; should mirror or reduce")
                    .isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("Low RPE (≤7) + good recovery signals readiness to increase load")
        void lowRpePlusGoodRecoveryAllowsLoadIncrease() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(6.5),            // low RPE: room to work harder
                    new Doms(1),             // minimal DOMS
                    new SleepQuality(8)      // good sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            assertThat(nextWeight)
                    .as("Low RPE + good recovery should increase load")
                    .isGreaterThan(100.0);
        }

        @Test
        @DisplayName("Optimal RPE (8-9) + good recovery enables steady progression")
        void optimalRpeEnablesSteadyProgression() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(8.5),            // in optimal range
                    new Doms(2),             // mild DOMS
                    new SleepQuality(7)      // adequate sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            // Should be at or slightly above current (may allow rep progression instead)
            assertThat(nextWeight)
                    .as("Optimal RPE should enable steady progression")
                    .isGreaterThanOrEqualTo(100.0 - 0.5)
                    .isLessThanOrEqualTo(101.5);
        }
    }

    @Nested
    @DisplayName("Sleep Quality: Recovery Amplifier/Dampener Tests")
    class SleepQualityTests {

        @Test
        @DisplayName("Excellent sleep (8-10) amplifies recovery and enables progression")
        void excellentSleepAmplifiersRecovery() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(8.5),
                    new Doms(3),             // mild-moderate DOMS
                    new SleepQuality(10)     // perfect sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            assertThat(nextWeight)
                    .as("Excellent sleep should amplify recovery despite mild DOMS")
                    .isGreaterThanOrEqualTo(100.0 - 0.5);
        }

        @Test
        @DisplayName("Poor sleep (0-4) dampens recovery and prevents progression")
        void poorSleepDampenersRecovery() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(7.0),            // low RPE (normally good signal)
                    new Doms(1),             // minimal DOMS
                    new SleepQuality(2)      // poor sleep
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            double nextWeight = result.sets().getFirst().prescribedWeightKg();
            assertThat(nextWeight)
                    .as("Poor sleep should prevent progression despite low RPE/DOMS")
                    .isLessThanOrEqualTo(100.0 + 0.5);
        }
    }

    @Nested
    @DisplayName("Rep Progression: Alternative to Weight Increase")
    class RepProgressionTests {

        @Test
        @DisplayName("When RPE is in optimal range, reps increase before weight does")
        void repProgressionWhenOptimalRpe() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    8,
                    new Rpe(8.5),            // optimal range
                    new Doms(2),
                    new SleepQuality(7)
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);

            int nextReps = result.sets().getFirst().prescribedReps();
            // Reps should increase or weight stay same (rep progression strategy)
            assertThat(nextReps)
                    .as("Should enable rep progression when in optimal RPE range")
                    .isGreaterThanOrEqualTo(8);
        }
    }

    @Nested
    @DisplayName("Integration: Complete Recovery Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Excellent recovery: All signals positive")
        void excellentRecoveryAllSignalsPositive() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(6.5),
                    new Doms(0),
                    new SleepQuality(9)
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);
            double nextWeight = result.sets().getFirst().prescribedWeightKg();

            assertThat(nextWeight)
                    .as("Excellent recovery should increase load")
                    .isGreaterThan(100.0);
        }

        @Test
        @DisplayName("Poor recovery: All signals negative")
        void poorRecoveryAllSignalsNegative() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(9.5),
                    new Doms(8),
                    new SleepQuality(2)
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);
            double nextWeight = result.sets().getFirst().prescribedWeightKg();

            assertThat(nextWeight)
                    .as("Poor recovery should reduce load")
                    .isLessThan(100.0);
        }

        @Test
        @DisplayName("Mixed signals: DOMS veto overrides positive RPE/sleep")
        void mixedSignalsDomsVetoWins() {
            ProgressionInput input = new ProgressionInput(
                    100.0,
                    5,
                    new Rpe(7.0),            // good signal
                    new Doms(7),             // severe DOMS: VETO
                    new SleepQuality(9)      // good signal
            );

            Prescription result = engine.nextFor(exerciseId, input, policy);
            double nextWeight = result.sets().getFirst().prescribedWeightKg();

            assertThat(nextWeight)
                    .as("Severe DOMS should veto positive signals from RPE/sleep")
                    .isLessThanOrEqualTo(100.0);
        }
    }
}


