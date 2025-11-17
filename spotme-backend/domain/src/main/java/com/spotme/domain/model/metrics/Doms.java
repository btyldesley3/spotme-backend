package com.spotme.domain.model.metrics;

//0=none, 1-3 mild, 4-6 moderate, 7-10 severe
public record Doms(int value) {
    public Doms {
        if (value < 0 || value > 10) {
            throw new IllegalArgumentException("DOMS value must be between 0 and 10.");
        }
    }
}
