package com.spotme.adapters.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alpha_email_allowlist")
public class EmailAllowlistEntity {

    @Id
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EmailAllowlistEntity() {}

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public boolean isActive() { return active; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
}

