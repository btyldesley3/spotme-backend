package com.spotme.domain.model.workout;

import com.spotme.domain.model.metrics.Rpe;

import java.util.Comparator;
import java.util.Objects;

public record SetEntry(int setNumber, int reps, double weightKg, Rpe rpe, String notes) {
    public SetEntry {
        if (setNumber < 1) {
            throw new IllegalArgumentException("SetNumber cannot be zero or negative.");
        }
        if (reps < 1) {
            throw new IllegalArgumentException("Reps cannot be zero or negative.");
        }
        if (weightKg < 0.0) {
            throw new IllegalArgumentException("WeightKg cannot be zero or negative.");
        }
        Objects.requireNonNull(rpe, "rpe");
    }

    /**
     * Defines the session "top set" as the heaviest completed set.
     * Ties are broken by higher RPE, then later set number.
     */
    public static Comparator<SetEntry> topSetComparator() {
        return Comparator.comparingDouble(SetEntry::weightKg)
                .thenComparing(entry -> entry.rpe().value())
                .thenComparingInt(SetEntry::setNumber);
    }
}
