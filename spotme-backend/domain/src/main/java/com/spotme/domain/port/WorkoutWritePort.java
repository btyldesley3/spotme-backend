package com.spotme.domain.port;

import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.model.workout.WorkoutSession;

public interface WorkoutWritePort {
    void savePrescription(UserId userId, Prescription prescription);

    void saveSession(WorkoutSession session);
}
