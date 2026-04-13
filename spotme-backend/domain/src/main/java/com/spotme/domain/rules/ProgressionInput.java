package com.spotme.domain.rules;

import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;
import com.spotme.domain.model.metrics.SleepQuality;

/**
 * Encapsulates all contextual data for progression algorithm to make
 * a next prescription. Includes recovery metrics (DOMS, RPE, sleep).
 */
public record ProgressionInput(
        double lastTopSetWeightKg,
        int lastTopSetReps,
        Rpe lastTopSetRpe,
        Doms doms,
        SleepQuality sleepQuality
) {

}
