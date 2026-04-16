package com.spotme.adapters.out.persistence.jpa.entity;

import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.TrainingGoal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false)
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "training_goal", nullable = false)
    private TrainingGoal trainingGoal;

    @Column(name = "baseline_sleep_hours", nullable = false)
    private int baselineSleepHours;

    @Column(name = "stress_sensitivity", nullable = false)
    private int stressSensitivity;

    @Column(name = "active_program_id")
    private UUID activeProgramId;

    public UserEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ExperienceLevel getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(ExperienceLevel experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public TrainingGoal getTrainingGoal() {
        return trainingGoal;
    }

    public void setTrainingGoal(TrainingGoal trainingGoal) {
        this.trainingGoal = trainingGoal;
    }

    public int getBaselineSleepHours() {
        return baselineSleepHours;
    }

    public void setBaselineSleepHours(int baselineSleepHours) {
        this.baselineSleepHours = baselineSleepHours;
    }

    public int getStressSensitivity() {
        return stressSensitivity;
    }

    public void setStressSensitivity(int stressSensitivity) {
        this.stressSensitivity = stressSensitivity;
    }

    public UUID getActiveProgramId() {
        return activeProgramId;
    }

    public void setActiveProgramId(UUID activeProgramId) {
        this.activeProgramId = activeProgramId;
    }
}


