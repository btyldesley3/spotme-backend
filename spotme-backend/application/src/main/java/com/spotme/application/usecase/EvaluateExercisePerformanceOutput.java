package com.spotme.application.usecase;

import java.util.List;

public class EvaluateExercisePerformanceOutput {
    private final String exerciseId;
    private final List<String> feedbackMessages;

    public EvaluateExercisePerformanceOutput(String exerciseId,
                                             List<String> feedbackMessages) {
        this.exerciseId = exerciseId;
        this.feedbackMessages = feedbackMessages;
    }

    public String getExerciseId() { return exerciseId; }
    public List<String> getFeedbackMessages() { return feedbackMessages; }
}
