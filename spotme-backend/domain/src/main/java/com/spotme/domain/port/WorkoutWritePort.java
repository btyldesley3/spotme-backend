package com.spotme.domain.port;

import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;

public interface WorkoutWritePort {
    void savePrescription(UserId userId, Prescription prescription);
}
