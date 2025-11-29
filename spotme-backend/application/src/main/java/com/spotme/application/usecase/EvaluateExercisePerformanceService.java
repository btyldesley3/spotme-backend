package com.spotme.application.usecase;

import com.spotme.domain.feedback.FeedbackEngine;
import com.spotme.domain.feedback.FeedbackResult;
import com.spotme.domain.model.exercise.ExerciseRepository;
import com.spotme.domain.model.exercise.Exercise;

public class EvaluateExercisePerformanceService implements EvaluateExercisePerformance {

    private final ExerciseRepository exerciseRepository;
    private final FeedbackEngine feedbackEngine;

    public EvaluateExercisePerformanceService(ExerciseRepository exerciseRepository,
                                              FeedbackEngine feedbackEngine) {
        this.exerciseRepository = exerciseRepository;
        this.feedbackEngine = feedbackEngine;
    }

    @Override
    public EvaluateExercisePerformanceOutput handle(EvaluateExercisePerformanceInput input) {
        Exercise exercise = exerciseRepository.findById(input.getExerciseId())
                .orElseThrow(() -> new IllegalArgumentException("Exercise not found: " + input.getExerciseId()));

        FeedbackResult result = feedbackEngine.evaluate(
                exercise,
                input.getSetsCompleted(),
                input.getRepsCompleted(),
                input.getWeightUsed(),
                input.getNotes()
        );

        return new EvaluateExercisePerformanceOutput(
                input.getExerciseId(),
                result.getMessages()
        );
    }
}
