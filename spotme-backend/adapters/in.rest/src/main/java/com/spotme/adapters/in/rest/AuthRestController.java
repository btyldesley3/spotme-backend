package com.spotme.adapters.in.rest;

import com.spotme.adapters.in.rest.security.JwtService;
import com.spotme.application.usecase.LoginUser;
import com.spotme.application.usecase.RegisterWithCredentials;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.RefreshTokenPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints — publicly accessible (no JWT required).
 * JWT generation lives here, not in application use cases, keeping domain transport-agnostic.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthRestController {

    private final RegisterWithCredentials registerWithCredentials;
    private final LoginUser loginUser;
    private final JwtService jwtService;
    private final RefreshTokenPort refreshTokenPort;

    public AuthRestController(
            RegisterWithCredentials registerWithCredentials,
            LoginUser loginUser,
            JwtService jwtService,
            RefreshTokenPort refreshTokenPort
    ) {
        this.registerWithCredentials = registerWithCredentials;
        this.loginUser = loginUser;
        this.jwtService = jwtService;
        this.refreshTokenPort = refreshTokenPort;
    }

    // ── Register ─────────────────────────────────────────────────────────────

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) {
        var result = registerWithCredentials.handle(new RegisterWithCredentials.Command(
                req.email(),
                req.password(),
                req.inviteCode(),
                req.experienceLevel(),
                req.trainingGoal(),
                req.baselineSleepHours(),
                req.stressSensitivity()
        ));
        return new RegisterResponse(result.userId().toString(), result.email());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        var result = loginUser.handle(new LoginUser.Command(req.email(), req.password()));
        return buildAuthResponse(result.userId().toString(), result.email());
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        var tokenHash = jwtService.hashToken(req.refreshToken());
        var userId = refreshTokenPort.validateAndConsume(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("refresh token is invalid or expired"));

        // Issue a new access token — we need the email for the claim.
        // For now we embed only userId in claims; front-end fetches profile separately if needed.
        var accessToken = jwtService.generateAccessToken(userId.toString(), "");
        var rawRefresh = jwtService.generateRefreshTokenValue();
        refreshTokenPort.save(userId, jwtService.hashToken(rawRefresh), jwtService.refreshTokenExpiry());

        return new AuthResponse(accessToken, rawRefresh);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * Revokes all outstanding refresh tokens for the authenticated user.
     * Requires a valid JWT Bearer token — the access token is the proof of identity.
     * The client should discard both the access token and any stored refresh tokens after calling this.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        revokeAllForAuthenticatedUser();
    }

    /**
     * Explicit logout-all endpoint for clients that want unambiguous semantics.
     * Currently equivalent to /logout and revokes all outstanding refresh tokens.
     */
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        revokeAllForAuthenticatedUser();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            String inviteCode,            // nullable
            @NotBlank String experienceLevel,
            @NotBlank String trainingGoal,
            int baselineSleepHours,
            int stressSensitivity
    ) {}

    public record RegisterResponse(String userId, String email) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(String accessToken, String refreshToken) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static class InvalidRefreshTokenException extends RuntimeException {
        public InvalidRefreshTokenException(String message) { super(message); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse buildAuthResponse(String userId, String email) {
        var accessToken = jwtService.generateAccessToken(userId, email);
        var rawRefresh = jwtService.generateRefreshTokenValue();
        refreshTokenPort.save(
                UserId.fromString(userId),
                jwtService.hashToken(rawRefresh),
                jwtService.refreshTokenExpiry()
        );
        return new AuthResponse(accessToken, rawRefresh);
    }

    private void revokeAllForAuthenticatedUser() {
        var userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        refreshTokenPort.revokeAllForUser(UserId.fromString(userId));
    }
}

