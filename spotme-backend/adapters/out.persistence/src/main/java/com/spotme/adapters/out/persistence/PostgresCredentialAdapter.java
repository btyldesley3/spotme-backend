package com.spotme.adapters.out.persistence;

import com.spotme.adapters.out.persistence.jpa.EmailAllowlistJpaRepository;
import com.spotme.adapters.out.persistence.jpa.InviteCodeJpaRepository;
import com.spotme.adapters.out.persistence.jpa.RefreshTokenJpaRepository;
import com.spotme.adapters.out.persistence.jpa.UserCredentialJpaRepository;
import com.spotme.adapters.out.persistence.jpa.entity.RefreshTokenEntity;
import com.spotme.adapters.out.persistence.jpa.entity.UserCredentialEntity;
import com.spotme.domain.model.user.AlphaAccessPath;
import com.spotme.domain.model.user.UserCredentials;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.AlphaAccessPort;
import com.spotme.domain.port.CredentialReadPort;
import com.spotme.domain.port.CredentialWritePort;
import com.spotme.domain.port.RefreshTokenPort;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Component
public class PostgresCredentialAdapter
        implements CredentialReadPort, CredentialWritePort, AlphaAccessPort, RefreshTokenPort {

    private final UserCredentialJpaRepository credentialRepo;
    private final InviteCodeJpaRepository inviteCodeRepo;
    private final EmailAllowlistJpaRepository allowlistRepo;
    private final RefreshTokenJpaRepository refreshTokenRepo;

    public PostgresCredentialAdapter(
            UserCredentialJpaRepository credentialRepo,
            InviteCodeJpaRepository inviteCodeRepo,
            EmailAllowlistJpaRepository allowlistRepo,
            RefreshTokenJpaRepository refreshTokenRepo) {
        this.credentialRepo = credentialRepo;
        this.inviteCodeRepo = inviteCodeRepo;
        this.allowlistRepo = allowlistRepo;
        this.refreshTokenRepo = refreshTokenRepo;
    }

    // ── CredentialReadPort ───────────────────────────────────────────────────

    @Override
    public Optional<UserCredentials> findByEmail(String email) {
        return credentialRepo.findByEmail(email.toLowerCase()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return credentialRepo.existsByEmail(email.toLowerCase());
    }

    // ── CredentialWritePort ──────────────────────────────────────────────────

    @Override
    public void save(UserCredentials credentials) {
        var entity = new UserCredentialEntity(
                credentials.userId().value(),
                credentials.email(),
                credentials.passwordHash(),
                credentials.alphaEligible(),
                credentials.alphaAccessPath() != null ? credentials.alphaAccessPath().name() : null,
                Instant.now()
        );
        credentialRepo.save(entity);
    }

    // ── AlphaAccessPort ──────────────────────────────────────────────────────

    @Override
    public boolean validateAndConsumeInviteCode(String rawCode) {
        var hash = sha256(rawCode);
        var found = inviteCodeRepo.findValidByCodeHash(hash, Instant.now());
        if (found.isEmpty()) return false;
        inviteCodeRepo.incrementUsedCount(found.get().getId());
        return true;
    }

    @Override
    public boolean isEmailAllowlisted(String email) {
        return allowlistRepo.findByEmailAndActiveTrue(email.toLowerCase()).isPresent();
    }

    // ── RefreshTokenPort ─────────────────────────────────────────────────────

    @Override
    public void save(UserId userId, String tokenHash, Instant expiresAt) {
        var entity = new RefreshTokenEntity(
                UUID.randomUUID(),
                userId.value(),
                tokenHash,
                expiresAt,
                false,
                Instant.now()
        );
        refreshTokenRepo.save(entity);
    }

    @Override
    public Optional<UserId> validateAndConsume(String tokenHash) {
        return refreshTokenRepo
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(tokenHash, Instant.now())
                .map(entity -> {
                    entity.setRevoked(true);
                    refreshTokenRepo.save(entity);
                    return new UserId(entity.getUserId());
                });
    }

    @Override
    public void revokeAllForUser(UserId userId) {
        refreshTokenRepo.revokeAllByUserId(userId.value());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UserCredentials toDomain(UserCredentialEntity e) {
        AlphaAccessPath path = e.getAlphaAccessPath() != null
                ? AlphaAccessPath.valueOf(e.getAlphaAccessPath())
                : null;
        return new UserCredentials(
                new UserId(e.getUserId()),
                e.getEmail(),
                e.getPasswordHash(),
                e.isAlphaEligible(),
                path
        );
    }

    private static String sha256(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}



