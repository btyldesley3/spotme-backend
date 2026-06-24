package com.spotme.adapters.in.grpc;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * gRPC transport-level token service used by auth endpoints and gRPC interceptors.
 */
@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlDays;

    public JwtTokenService(
            @Value("${spotme.jwt.secret}") String secret,
            @Value("${spotme.jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes,
            @Value("${spotme.jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("spotme.jwt.secret must be configured as a base64-encoded key");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("spotme.jwt.secret must be valid base64", ex);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("spotme.jwt.secret must decode to at least 32 bytes (256-bit key)");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public String generateAccessToken(String userId, String email) {
        var now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlMinutes * 60)))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plusSeconds(refreshTokenTtlDays * 86_400);
    }

    public String hashToken(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public Claims validateAccessToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(String token) {
        return validateAccessToken(token).getSubject();
    }
}

