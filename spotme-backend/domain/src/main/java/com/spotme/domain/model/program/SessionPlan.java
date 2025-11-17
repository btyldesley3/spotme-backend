package com.spotme.domain.model.program;

import com.spotme.domain.model.plan.Prescription;

import java.util.Objects;

public class SessionPlan {

    private final int sessionNumber;
    private final Prescription prescription;

    public SessionPlan(int sessionNumber, Prescription prescription) {
        this.sessionNumber = sessionNumber;
        this.prescription = Objects.requireNonNull(prescription);
    }

    public int getSessionNumber() {
        return sessionNumber;
    }

    public Prescription getPrescription() {
        return prescription;
    }

}
