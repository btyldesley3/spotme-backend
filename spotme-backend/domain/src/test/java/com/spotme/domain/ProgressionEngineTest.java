package com.spotme.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;
import com.spotme.domain.rules.ProgressionEngine;
import com.spotme.domain.rules.ProgressionInput;
import com.spotme.domain.rules.ProgressionPolicy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgressionEngineTest {
    @Test
    void microLoadsWhenDomsNotSevere() throws Exception {
        var json = """
      {"progression_logic":{"micro_loading_policy":{"barbell_upper":{"min_inc":2.5}},"rpe_thresholds":{"low":7.0,"optimal_min":8.0,"optimal_max":9.0,"high":9.5}},
       "recovery":{"doms":{"severe_threshold":7,"moderate_threshold":4},"sleep":{"poor_threshold":4},"weighting":{"doms_impact":0.5,"rpe_influence":0.3,"sleep_influence":0.2},"safety":{"min_load_reduction_pct":2.5}}}
    """;
        var rules = new ObjectMapper().readTree(json);
        var policy = ProgressionPolicy.fromJson(rules, "barbell_upper");

        var engine = new ProgressionEngine();
        var input = new ProgressionInput(60.0, 8, new Rpe(8.5), new Doms(3), new SleepQuality(7));
        var rx = engine.nextFor(new ExerciseId(UUID.randomUUID()), input, policy);

        assertThat(rx.sets()).singleElement().satisfies(s -> {
            assertThat(s.prescribedWeightKg()).isEqualTo(60.0); // Mirror with good sleep and moderate DOMS
            assertThat(s.prescribedReps()).isGreaterThanOrEqualTo(8);
        });
    }
}
