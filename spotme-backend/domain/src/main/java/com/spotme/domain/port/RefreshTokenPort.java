package com.spotme.domain.port;

import com.spotme.domain.model.user.UserId;

import java.time.Instant;
import java.util.Optional;

/**
 * Port for persisting and validating opaque refresh tokens.
 * Only the SHA-256 hash of the token is stored; the raw value is returned to the client once.
 */
public interface RefreshTokenPort {

    /** Persists a new refresh token record (store the hash, not the raw value). */
    void save(UserId userId, String tokenHash, Instant expiresAt);

    /**
     * Looks up a non-revoked, non-expired token by its hash.
     * If found, revokes it (one-time use) and returns the associated userId.
     */
    Optional<UserId> validateAndConsume(String tokenHash);

    /** Revokes all outstanding refresh tokens for the user (e.g. on logout). */
    void revokeAllForUser(UserId userId);
}

