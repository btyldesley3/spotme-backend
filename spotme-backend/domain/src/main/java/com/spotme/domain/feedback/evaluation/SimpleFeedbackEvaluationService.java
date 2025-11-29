package com.spotme.domain.feedback.evaluation;

import com.spotme.domain.feedback.model.AdjustmentType;
import com.spotme.domain.feedback.model.FeedbackInputs;
import com.spotme.domain.feedback.model.TrainingAdjustment;

public class SimpleFeedbackEvaluationService implements FeedbackEvaluationService {

    // This logic is a work in progress and intended to be iterated upon, I'm not happy with the current evaluation
    // logic and this will be changed after we have a working version

    @Override
    public TrainingAdjustment evaluateFeedback(FeedbackInputs inputs) {
        // Global override: Poor recovery → reduce intensity
        if (inputs.sleepQuality() <= 4 || inputs.doms() >= 7) {
            return new TrainingAdjustment(
                    AdjustmentType.DECREASE_INTENSITY,
                    -10,   // reduce load by 10%
                    -1     // remove one set
            );
        }

        // High RPE = hard effort → deload slightly
        if (inputs.rpe() >= 9) {
            return new TrainingAdjustment(
                    AdjustmentType.DECREASE_INTENSITY,
                    -5,
                    0
            );
        }

        // Target zone – maintain
        if (inputs.rpe() >= 7) {
            return new TrainingAdjustment(
                    AdjustmentType.MAINTAIN_INTENSITY,
                    0,
                    0
            );
        }

        // Low RPE → training too easy → increase intensity
        return new TrainingAdjustment(
                AdjustmentType.INCREASE_INTENSITY,
                +5,
                +1
        );
    }
}
