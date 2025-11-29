package com.spotme.domain.feedback.model;

public record TrainingAdjustment(
        AdjustmentType type,
        int loadDeltaPercentage, // e.g., +5% or -10%
        int volumeDeltaSets // e.g., +1 set, -1 set
) {

}
