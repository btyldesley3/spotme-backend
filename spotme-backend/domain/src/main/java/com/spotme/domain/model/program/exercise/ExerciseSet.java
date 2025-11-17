package com.spotme.domain.model.program.exercise;

public class ExerciseSet {

    private final SetId id;
    private final int reps;
    private final double targetRpe;
    private final double load;
    private final int restTimeSeconds;

    public ExerciseSet(SetId id, int reps, double targetRpe, double load, int restTimeSeconds) {
        this.id = id;
        this.reps = reps;
        this.targetRpe = targetRpe;
        this.load = load;
        this.restTimeSeconds = restTimeSeconds;
    }

    public SetId getId() {
        return id;
    }

}
