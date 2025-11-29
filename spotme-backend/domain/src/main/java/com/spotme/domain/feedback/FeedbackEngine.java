package com.spotme.domain.feedback;

import com.spotme.domain.model.exercise.Exercise;

import java.util.List;

public interface FeedbackEngine {
    FeedbackResult evaluate (Exercise exercise,
                             int setsCompleted,
                             int repsCompleted,
                             double weightUsed,
                             List<String> notes);
}
