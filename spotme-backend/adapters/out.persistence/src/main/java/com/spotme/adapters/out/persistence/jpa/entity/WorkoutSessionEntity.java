package com.spotme.adapters.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_sessions")
public class WorkoutSessionEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "doms")
    private Integer doms;

    @Column(name = "sleep_quality")
    private Integer sleepQuality;

    @Column(name = "min_total_sets")
    private Integer minTotalSets;

    @Column(name = "min_distinct_exercises")
    private Integer minDistinctExercises;

    @Column(name = "min_sets_per_exercise")
    private Integer minSetsPerExercise;

    @Column(name = "require_recovery_feedback_for_progression")
    private Boolean requireRecoveryFeedbackForProgression;

    public WorkoutSessionEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getDoms() {
        return doms;
    }

    public void setDoms(Integer doms) {
        this.doms = doms;
    }

    public Integer getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(Integer sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public Integer getMinTotalSets() {
        return minTotalSets;
    }

    public void setMinTotalSets(Integer minTotalSets) {
        this.minTotalSets = minTotalSets;
    }

    public Integer getMinDistinctExercises() {
        return minDistinctExercises;
    }

    public void setMinDistinctExercises(Integer minDistinctExercises) {
        this.minDistinctExercises = minDistinctExercises;
    }

    public Integer getMinSetsPerExercise() {
        return minSetsPerExercise;
    }

    public void setMinSetsPerExercise(Integer minSetsPerExercise) {
        this.minSetsPerExercise = minSetsPerExercise;
    }

    public Boolean getRequireRecoveryFeedbackForProgression() {
        return requireRecoveryFeedbackForProgression;
    }

    public void setRequireRecoveryFeedbackForProgression(Boolean requireRecoveryFeedbackForProgression) {
        this.requireRecoveryFeedbackForProgression = requireRecoveryFeedbackForProgression;
    }
}


