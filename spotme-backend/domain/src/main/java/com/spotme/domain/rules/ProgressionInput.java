package com.spotme.domain.rules;

import com.spotme.domain.model.metrics.Doms;
import com.spotme.domain.model.metrics.Rpe;

public record ProgressionInput(
        double lastTopSetWeightKg,
        int lastTopSetReps,
        Rpe lastTopSetRpe,
        Doms doms
) {

}
