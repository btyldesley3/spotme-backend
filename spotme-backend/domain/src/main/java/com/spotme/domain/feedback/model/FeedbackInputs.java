package com.spotme.domain.feedback.model;

public record FeedbackInputs(
        int rpe, // 1-10
        int doms, // 0-10
        int sleepQuality // 0-10
) {
    public FeedbackInputs {
        if (rpe < 1 || rpe > 10) {
            throw new IllegalArgumentException("RPE must be between 1 and 10");
        }
        if (doms < 0 || doms > 10) {
            throw new IllegalArgumentException("DOMS must be between 0 and 10");
        }
        if (sleepQuality < 0 || sleepQuality > 10) {
            throw new IllegalArgumentException("Sleep Quality must be between 0 and 10");
        }
    }

}
