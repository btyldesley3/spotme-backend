package com.spotme.adapters.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class UserCredentialEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "alpha_eligible", nullable = false)
    private boolean alphaEligible;

    @Column(name = "alpha_access_path")
    private String alphaAccessPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserCredentialEntity() {}

    public UserCredentialEntity(UUID userId, String email, String passwordHash,
                                boolean alphaEligible, String alphaAccessPath, Instant createdAt) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.alphaEligible = alphaEligible;
        this.alphaAccessPath = alphaAccessPath;
        this.createdAt = createdAt;
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isAlphaEligible() { return alphaEligible; }
    public String getAlphaAccessPath() { return alphaAccessPath; }
    public Instant getCreatedAt() { return createdAt; }
}

