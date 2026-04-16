package com.spotme.adapters.out.persistence.jpa.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workouts")
public class WorkoutEntity {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "block_id", nullable = false, columnDefinition = "UUID")
    private UUID blockId;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "session_number", nullable = false)
    private Integer sessionNumber;

    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "set_presets_json", nullable = false, columnDefinition = "TEXT")
    private String setPresetsJson;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private Instant updatedAt;

    // Constructors
    public WorkoutEntity() {
    }

    public WorkoutEntity(UUID id, UUID blockId, Integer weekNumber, Integer sessionNumber,
                        Integer version, String notes, String setPresetsJson) {
        this.id = id;
        this.blockId = blockId;
        this.weekNumber = weekNumber;
        this.sessionNumber = sessionNumber;
        this.version = version;
        this.notes = notes;
        this.setPresetsJson = setPresetsJson;
    }

    // Accessors
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBlockId() {
        return blockId;
    }

    public void setBlockId(UUID blockId) {
        this.blockId = blockId;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public void setSessionNumber(Integer sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getSetPresetsJson() {
        return setPresetsJson;
    }

    public void setSetPresetsJson(String setPresetsJson) {
        this.setPresetsJson = setPresetsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

