package com.spotme.domain.model.user;

import com.spotme.domain.model.program.ProgramId;

import java.util.Objects;

public class User {

    private final UserId id;
    private final ExperienceLevel experienceLevel;
    private final TrainingGoal trainingGoal;
    private final RecoveryProfile recoveryProfile;
    private ProgramId activeProgramId;

    public User(
            UserId id,
            ExperienceLevel experienceLevel,
            TrainingGoal trainingGoal,
            RecoveryProfile recoveryProfile
    ) {
        this.id = Objects.requireNonNull(id);
        this.experienceLevel = Objects.requireNonNull(experienceLevel);
        this.trainingGoal = Objects.requireNonNull(trainingGoal);
        this.recoveryProfile = Objects.requireNonNull(recoveryProfile);
    }

    public UserId id() {
        return id;
    }

    public ExperienceLevel experienceLevel() {
        return experienceLevel;
    }

    public TrainingGoal trainingGoal() {
        return trainingGoal;
    }

    public RecoveryProfile recoveryProfile() {
        return recoveryProfile;
    }

    public ProgramId activeProgramId() {
        return activeProgramId;
    }

    public void assignProgram(ProgramId programId) {
        this.activeProgramId = programId;
    }

}
