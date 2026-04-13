package com.spotme.domain.rules;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Immutable progression policy parsed from JSON.
 * Dictates how DOMS, RPE, and sleep thresholds influence load adjustments.
 */
public record ProgressionPolicy(
        double microLoadKg,
        int severeDomsThreshold,
        int moderateDomsThreshold,
        double lowRpeThreshold,
        double optimalRpeMin,
        double optimalRpeMax,
        double highRpeThreshold,
        int poorSleepThreshold,
        double loadIncreaseWeightRpe,
        double loadIncreaseWeightSleep,
        double loadDecreaseWeightDoms,
        double minLoadReductionPct
) {
    public static ProgressionPolicy fromJson(JsonNode rules, String modalityKey) {
        var progressionNode = rules.path("progression_logic");
        var recoveryNode = rules.path("recovery");
        var micro = progressionNode.path("micro_loading_policy").path(modalityKey);

        double microLoadInc = micro.path("min_inc").asDouble(1.25);
        int severeDomsThresh = recoveryNode.path("doms").path("severe_threshold").asInt(7);
        int moderateDomsThresh = recoveryNode.path("doms").path("moderate_threshold").asInt(4);
        double lowRpeThresh = progressionNode.path("rpe_thresholds").path("low").asDouble(7.0);
        double optimalMin = progressionNode.path("rpe_thresholds").path("optimal_min").asDouble(8.0);
        double optimalMax = progressionNode.path("rpe_thresholds").path("optimal_max").asDouble(9.0);
        double highRpeThresh = progressionNode.path("rpe_thresholds").path("high").asDouble(9.5);
        int poorSleepThresh = recoveryNode.path("sleep").path("poor_threshold").asInt(4);
        double rpeWeight = recoveryNode.path("weighting").path("rpe_influence").asDouble(0.3);
        double sleepWeight = recoveryNode.path("weighting").path("sleep_influence").asDouble(0.2);
        double domsWeight = recoveryNode.path("weighting").path("doms_impact").asDouble(0.5);
        double minReduction = recoveryNode.path("safety").path("min_load_reduction_pct").asDouble(2.5);

        return new ProgressionPolicy(
                microLoadInc,
                severeDomsThresh,
                moderateDomsThresh,
                lowRpeThresh,
                optimalMin,
                optimalMax,
                highRpeThresh,
                poorSleepThresh,
                rpeWeight,
                sleepWeight,
                domsWeight,
                minReduction
        );
    }
}
