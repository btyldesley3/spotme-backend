package com.spotme.domain.model.plan;

import java.util.List;

public record Prescription(
        List<SetPrescription> sets
) {
    public Prescription {
        if (sets == null || sets.isEmpty()) {
            throw new IllegalArgumentException("Prescription must contain at least one SetPrescription");
        }
    }
}
