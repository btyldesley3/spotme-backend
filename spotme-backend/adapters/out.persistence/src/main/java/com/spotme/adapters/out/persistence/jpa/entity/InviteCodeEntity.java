package com.spotme.adapters.out.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alpha_invite_codes")
public class InviteCodeEntity {

    @Id
    private UUID id;

    @Column(name = "code_hash", nullable = false, unique = true)
    private String codeHash;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "max_uses", nullable = false)
    private int maxUses;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected InviteCodeEntity() {}

    public UUID getId() { return id; }
    public String getCodeHash() { return codeHash; }
    public boolean isActive() { return active; }
    public int getMaxUses() { return maxUses; }
    public int getUsedCount() { return usedCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setUsedCount(int usedCount) { this.usedCount = usedCount; }
    public void setActive(boolean active) { this.active = active; }
}

