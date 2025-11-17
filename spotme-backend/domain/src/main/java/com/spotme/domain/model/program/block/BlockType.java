package com.spotme.domain.model.program.block;

public enum BlockType {
    HYPERTROPHY(8, 20, 7.5, 9),
    STRENGTH(3, 6, 7.0, 10.0),
    DELOAD(5, 15, 4.0, 6.0),
    INTRO(8, 15, 5.0, 7.0),
    SPECIALIZATION(8, 20, 6.0, 9.0),
    PEAKING(1, 3, 8.0, 10.0);

    private final int minReps;
    private final int maxReps;
    private final double minRpe;
    private final double maxRpe;

    BlockType(int minReps, int maxReps, double minRpe, double maxRpe) {
        this.minReps = minReps;
        this.maxReps = maxReps;
        this.minRpe = minRpe;
        this.maxRpe = maxRpe;
    }

    public int minReps() {
        return minReps;
    }

    public int maxReps() {
        return maxReps;
    }

    public double minRpe() {
        return minRpe;
    }

    public double maxRpe() {
        return maxRpe;
    }
}

