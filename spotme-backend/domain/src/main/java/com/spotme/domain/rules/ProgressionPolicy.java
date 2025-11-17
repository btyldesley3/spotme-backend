package com.spotme.domain.rules;

import com.fasterxml.jackson.databind.JsonNode;

/** Immutable progression policy parsed from dynamic algorithm in JSON. */
public record ProgressionPolicy(
        double microLoadKg,
        int severeDomsThreshold
) {
    public static ProgressionPolicy fromJson(JsonNode rules, String modalityKey) {
        var micro = rules.path("progression_logic").path("micro_loading_policy").path(modalityKey);
        double inc = micro.path("min_inc").asDouble(1.25);
        int severe = rules.path("recovery").path("doms").path("severe_threshold").asInt(7);
        return new ProgressionPolicy(inc, severe);
    }
}
