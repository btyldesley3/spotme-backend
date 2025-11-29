package com.spotme.application.usecase;

import java.util.List;

public class EvaluateExercisePerformanceInput {
    private final String exerciseId;
    private final int setsCompleted;
    private final int repsCompleted;
    private final double weightUsed;
    private final List<String> notes;

    public EvaluateExercisePerformanceInput(String exerciseId,
                                            int setsCompleted,
                                            int repsCompleted,
                                            double weightUsed,
                                            List<String> notes) {
        this.exerciseId = exerciseId;
        this.setsCompleted = setsCompleted;
        this.repsCompleted = repsCompleted;
        this.weightUsed = weightUsed;
        this.notes = notes;
    }

    public String getExerciseId() { return exerciseId; }
    public int getSetsCompleted() { return setsCompleted; }
    public int getRepsCompleted() { return repsCompleted; }
    public double getWeightUsed() { return weightUsed; }
    public List<String> getNotes() { return notes; }
}
