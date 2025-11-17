package com.spotme.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
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
      {"progression_logic":{"micro_loading_policy":{"barbell_upper":{"min_inc":2.5}}},
       "recovery":{"doms":{"severe_threshold":7}}}
    """;
        var rules = new ObjectMapper().readTree(json);
        var policy = ProgressionPolicy.fromJson(rules, "barbell_upper");

        var engine = new ProgressionEngine();
        var input = new ProgressionInput(60.0, 8, new Rpe(8.5), new Doms(3));
        var rx = engine.nextFor(new ExerciseId(UUID.randomUUID()), input, policy);

        assertThat(rx.sets()).singleElement().satisfies(s -> {
            assertThat(s.prescribedWeightKg()).isEqualTo(62.5);
            assertThat(s.prescribedReps()).isEqualTo(8);
        });
    }
}
