package com.spotme.domain.feedback.evaluation;

import com.spotme.domain.feedback.model.FeedbackInputs;
import com.spotme.domain.feedback.model.TrainingAdjustment;

public interface FeedbackEvaluationService {

    TrainingAdjustment evaluateFeedback(FeedbackInputs inputs);
}
