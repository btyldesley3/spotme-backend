package com.spotme.domain.model.metrics;

// 0=no sleep, 1-3 poor, 4-6 fair, 7-10 excellent
public record SleepQuality(int value) {
    public SleepQuality {
        if (value < 0 || value > 10) {
            throw new IllegalArgumentException("Sleep quality value must be between 0 and 10.");
        }
    }

    /**
     * Normalized sleep recovery factor (0.5 to 1.5).
     * Poor sleep reduces recovery capacity; excellent sleep enhances it.
     */
    public double recoveryFactor() {
        // Map [0, 10] -> [0.5, 1.5] recovery multiplier
        return 0.5 + (value / 10.0);
    }
}

