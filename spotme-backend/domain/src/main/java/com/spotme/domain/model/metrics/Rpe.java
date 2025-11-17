package com.spotme.domain.model.metrics;

//Rate of Perceived Exertion (RPE) scale from 0 to 10
public record Rpe(double value) {
    public Rpe {
        if (value < 0.0 || value > 10.0) {
            throw new IllegalArgumentException("RPE value must be between 0 and 10.");
        }
    }
}
