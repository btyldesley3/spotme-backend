package com.spotme;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.enabled=true",
        "grpc.server.port=9090"
})
@ActiveProfiles("test")
@Import(AuthFlowRestIntegrationTest.EmbeddedPostgresConfig.class)
class AuthFlowRestIntegrationTest {

    /** A stable UUID used as an exercise identifier across workout-flow tests. */
    private static final String EXERCISE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ALLOWLIST_EMAIL_1 = "alpha-tester@spotme.dev";
    private static final String ALLOWLIST_EMAIL_2 = "alpha-tester2@spotme.dev";
    private static final String INVITE_ONLY_EMAIL_1 = "invite-only-1@spotme.dev";
    private static final String INVITE_ONLY_EMAIL_2 = "invite-only-2@spotme.dev";
    private static final UUID ALLOWLIST_ID_1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID ALLOWLIST_ID_2 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID INVITE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String INVITE_CODE = "ALPHA-INVITE-ONE";

    private static final EmbeddedPostgres POSTGRES = startEmbeddedPostgres();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUpAllowlist() {
        jdbc.update("delete from user_credentials where email in (?, ?, ?, ?)",
                ALLOWLIST_EMAIL_1, ALLOWLIST_EMAIL_2, INVITE_ONLY_EMAIL_1, INVITE_ONLY_EMAIL_2);

        jdbc.update(
                "insert into alpha_email_allowlist (id, email, active, notes, created_at) values (?, ?, true, ?, now()) " +
                        "on conflict (email) do update set active = excluded.active, notes = excluded.notes",
                ALLOWLIST_ID_1, ALLOWLIST_EMAIL_1, "integration-test"
        );
        jdbc.update(
                "insert into alpha_email_allowlist (id, email, active, notes, created_at) values (?, ?, true, ?, now()) " +
                        "on conflict (email) do update set active = excluded.active, notes = excluded.notes",
                ALLOWLIST_ID_2, ALLOWLIST_EMAIL_2, "integration-test-user2"
        );

        jdbc.update("delete from alpha_invite_codes where id = ?", INVITE_ID);
        jdbc.update(
                "insert into alpha_invite_codes (id, code_hash, active, max_uses, used_count, expires_at, created_at) " +
                        "values (?, ?, true, 1, 0, ?, now())",
                INVITE_ID,
                sha256(INVITE_CODE),
                Timestamp.from(Instant.now().plusSeconds(3600))
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    record AuthResult(String userId, String accessToken, String refreshToken) {}

    private AuthResult registerAndLogin(String email) {
        var registerReq = Map.of(
                "email", email,
                "password", "Password123!",
                "experienceLevel", "beginner",
                "trainingGoal", "strength",
                "baselineSleepHours", 7,
                "stressSensitivity", 3
        );
        ResponseEntity<Map> registerRes = rest.postForEntity("/api/auth/register", registerReq, Map.class);
        assertEquals(HttpStatus.CREATED, registerRes.getStatusCode(),
                () -> "register failed for " + email + " body=" + registerRes.getBody());
        assertNotNull(registerRes.getBody(), "register body should not be null");
        var userId = (String) registerRes.getBody().get("userId");
        assertNotNull(userId, "register response must contain userId");

        var loginReq = Map.of("email", email, "password", "Password123!");
        ResponseEntity<Map> loginRes = rest.postForEntity("/api/auth/login", loginReq, Map.class);
        assertEquals(HttpStatus.OK, loginRes.getStatusCode(),
                () -> "login failed for " + email + " body=" + loginRes.getBody());
        assertNotNull(loginRes.getBody(), "login body should not be null");
        var accessToken = (String) loginRes.getBody().get("accessToken");
        var refreshToken = (String) loginRes.getBody().get("refreshToken");
        assertNotNull(accessToken, "login response must contain accessToken");
        assertNotNull(refreshToken, "login response must contain refreshToken");
        return new AuthResult(userId, accessToken, refreshToken);
    }

    private HttpHeaders bearerHeaders(String token) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpEntity<Void> authedRequest(String token) {
        return new HttpEntity<>(null, bearerHeaders(token));
    }

    private HttpEntity<Map<String, Object>> authedJson(String token, Map<String, Object> body) {
        return new HttpEntity<>(body, bearerHeaders(token));
    }

    private String alphaAccessPathForEmail(String email) {
        return jdbc.queryForObject(
                "select alpha_access_path from user_credentials where email = ?",
                String.class,
                email
        );
    }

    private static String sha256(String rawValue) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void alphaAllowlistedUserCanRegisterLoginAndCallProtectedEndpoint() {
        var auth = registerAndLogin(ALLOWLIST_EMAIL_1);

        assertNotNull(auth.userId());
        assertNotNull(auth.accessToken());

        // Protected route should reject missing JWT
        ResponseEntity<String> noAuthRes = rest.getForEntity("/api/v1/users/" + auth.userId(), String.class);
        assertTrue(noAuthRes.getStatusCode().is4xxClientError());

        // Same route succeeds with Bearer token
        var authReq = authedRequest(auth.accessToken());
        ResponseEntity<Map> userRes = rest.exchange("/api/v1/users/" + auth.userId(), HttpMethod.GET, authReq, Map.class);
        assertEquals(HttpStatus.OK, userRes.getStatusCode());
        assertEquals(auth.userId(), userRes.getBody().get("userId"));
        assertEquals("EMAIL_ALLOWLIST", alphaAccessPathForEmail(ALLOWLIST_EMAIL_1));
    }

    @Test
    void registrationIsDeniedWhenNotAllowlistedAndNoValidInviteCode() {
        var registerReq = Map.of(
                "email", "not-allowed@spotme.dev",
                "password", "Password123!",
                "experienceLevel", "beginner",
                "trainingGoal", "strength",
                "baselineSleepHours", 7,
                "stressSensitivity", 3,
                "inviteCode", "invalid-code"
        );

        ResponseEntity<Map> registerRes = rest.postForEntity("/api/auth/register", registerReq, Map.class);
        assertEquals(HttpStatus.FORBIDDEN, registerRes.getStatusCode());
    }

    @Test
    void inviteCodeAllowsRegistrationAndSingleUseCodeIsConsumed() {
        var registerReq = Map.of(
                "email", INVITE_ONLY_EMAIL_1,
                "password", "Password123!",
                "experienceLevel", "beginner",
                "trainingGoal", "strength",
                "baselineSleepHours", 7,
                "stressSensitivity", 3,
                "inviteCode", INVITE_CODE
        );

        ResponseEntity<Map> registerRes = rest.postForEntity("/api/auth/register", registerReq, Map.class);
        assertEquals(HttpStatus.CREATED, registerRes.getStatusCode());
        assertEquals("INVITE_CODE", alphaAccessPathForEmail(INVITE_ONLY_EMAIL_1));

        Integer usedCount = jdbc.queryForObject(
                "select used_count from alpha_invite_codes where id = ?",
                Integer.class,
                INVITE_ID
        );
        assertEquals(1, usedCount);

        var secondAttemptReq = Map.of(
                "email", INVITE_ONLY_EMAIL_2,
                "password", "Password123!",
                "experienceLevel", "beginner",
                "trainingGoal", "strength",
                "baselineSleepHours", 7,
                "stressSensitivity", 3,
                "inviteCode", INVITE_CODE
        );
        ResponseEntity<Map> secondAttemptRes = rest.postForEntity("/api/auth/register", secondAttemptReq, Map.class);
        assertEquals(HttpStatus.FORBIDDEN, secondAttemptRes.getStatusCode());
    }

    @Test
    void crossUserSpoofingIsForbidden() {
        var user1 = registerAndLogin(ALLOWLIST_EMAIL_1);
        var user2 = registerAndLogin(ALLOWLIST_EMAIL_2);

        // user2 can access their own profile
        ResponseEntity<Map> ownProfile = rest.exchange(
                "/api/v1/users/" + user2.userId(),
                HttpMethod.GET,
                authedRequest(user2.accessToken()),
                Map.class
        );
        assertEquals(HttpStatus.OK, ownProfile.getStatusCode());

        // user1 trying to read user2's profile → 403 Forbidden
        ResponseEntity<Map> spoofAttempt = rest.exchange(
                "/api/v1/users/" + user2.userId(),
                HttpMethod.GET,
                authedRequest(user1.accessToken()),
                Map.class
        );
        assertEquals(HttpStatus.FORBIDDEN, spoofAttempt.getStatusCode());
    }

    @Test
    void logoutRevokesRefreshToken() {
        var auth = registerAndLogin(ALLOWLIST_EMAIL_1);

        // Logout — revokes all refresh tokens server-side
        var logoutRes = rest.exchange("/api/auth/logout", HttpMethod.POST,
                authedRequest(auth.accessToken()), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, logoutRes.getStatusCode());

        // Refresh token should now be invalid
        var refreshReq = Map.of("refreshToken", auth.refreshToken());
        ResponseEntity<Map> refreshRes = rest.postForEntity("/api/auth/refresh", refreshReq, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, refreshRes.getStatusCode());
    }

    @Test
    void logoutAllRevokesRefreshToken() {
        var auth = registerAndLogin(ALLOWLIST_EMAIL_1);

        // Logout-all — explicit endpoint for revoking all refresh tokens for the current user
        var logoutAllRes = rest.exchange("/api/auth/logout-all", HttpMethod.POST,
                authedRequest(auth.accessToken()), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, logoutAllRes.getStatusCode());

        // Refresh token should now be invalid
        var refreshReq = Map.of("refreshToken", auth.refreshToken());
        ResponseEntity<Map> refreshRes = rest.postForEntity("/api/auth/refresh", refreshReq, Map.class);
        assertEquals(HttpStatus.UNAUTHORIZED, refreshRes.getStatusCode());
    }

    @Test
    void logoutAllRequiresAuthentication() {
        ResponseEntity<String> noAuthRes = rest.postForEntity("/api/auth/logout-all", null, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, noAuthRes.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullAuthenticatedWorkoutFlowFromRegisterToRecommendation() {
        var auth = registerAndLogin(ALLOWLIST_EMAIL_1);
        var token = auth.accessToken();

        // 1. Start a workout session (body is optional — startedAt can be omitted)
        var startBody = Map.<String, Object>of("startedAt", "2026-04-26T09:00:00Z");
        ResponseEntity<Map> startRes = rest.exchange(
                "/api/v1/workout-sessions/start", HttpMethod.POST,
                authedJson(token, startBody), Map.class);
        assertEquals(HttpStatus.OK, startRes.getStatusCode(),
                () -> "start response body=" + startRes.getBody());
        var sessionId = (String) startRes.getBody().get("sessionId");
        assertNotNull(sessionId);
        assertEquals(auth.userId(), startRes.getBody().get("userId"));

        // 2. Log first set
        var logSet1 = Map.<String, Object>of("exerciseId", EXERCISE_ID, "setNumber", 1,
                "reps", 8, "weightKg", 57.5, "rpe", 7.0);
        ResponseEntity<Map> log1Res = rest.exchange(
                "/api/v1/workout-sessions/" + sessionId + "/sets", HttpMethod.POST,
                authedJson(token, logSet1), Map.class);
        assertEquals(HttpStatus.OK, log1Res.getStatusCode());
        assertEquals(1, log1Res.getBody().get("totalSetsInSession"));

        // 3. Log top set
        var logSet2 = Map.<String, Object>of("exerciseId", EXERCISE_ID, "setNumber", 2,
                "reps", 8, "weightKg", 60.0, "rpe", 6.5);
        ResponseEntity<Map> log2Res = rest.exchange(
                "/api/v1/workout-sessions/" + sessionId + "/sets", HttpMethod.POST,
                authedJson(token, logSet2), Map.class);
        assertEquals(HttpStatus.OK, log2Res.getStatusCode());
        assertEquals(2, log2Res.getBody().get("totalSetsInSession"));

        // 4. Complete session with recovery feedback
        var completeBody = Map.<String, Object>of(
                "finishedAt", "2026-04-26T09:45:00Z",
                "minTotalSets", 2,
                "minDistinctExercises", 1,
                "minSetsPerExercise", 2,
                "requireRecoveryFeedbackForProgression", true,
                "doms", 2,
                "sleepQuality", 8
        );
        ResponseEntity<Map> completeRes = rest.exchange(
                "/api/v1/workout-sessions/" + sessionId + "/complete", HttpMethod.POST,
                authedJson(token, completeBody), Map.class);
        assertEquals(HttpStatus.OK, completeRes.getStatusCode());
        assertTrue((Boolean) completeRes.getBody().get("completed"));
        assertTrue((Boolean) completeRes.getBody().get("allowsProgression"));
        assertEquals(2, completeRes.getBody().get("totalSets"));

        // 5. Request adaptive recommendation — should micro-load from the completed session
        var recommendBody = Map.<String, Object>of(
                "exerciseId", EXERCISE_ID,
                "rulesVersion", "v1.0.0",
                "modalityKey", "barbell_upper"
        );
        ResponseEntity<Map> recommendRes = rest.exchange(
                "/api/v1/recommendations", HttpMethod.POST,
                authedJson(token, recommendBody), Map.class);
        assertEquals(HttpStatus.OK, recommendRes.getStatusCode());
        var sets = (List<Map<String, Object>>) recommendRes.getBody().get("sets");
        assertFalse(sets.isEmpty(), "Recommendation should contain at least one set prescription");
        var firstSet = sets.get(0);
        assertEquals(EXERCISE_ID, firstSet.get("exerciseId"));
        assertEquals(8, firstSet.get("prescribedReps"));
        // STEADY_PROGRESSION signal: mirrorWithRepsGain — 60kg + (1.25 microload * 0.5) = 60.75kg
        assertEquals(60.75, (Double) firstSet.get("prescribedWeightKg"), 0.01);

        // 6. View recent sessions — should contain the completed session
        ResponseEntity<Map> recentRes = rest.exchange(
                "/api/v1/workout-sessions/recent", HttpMethod.GET,
                authedRequest(token), Map.class);
        assertEquals(HttpStatus.OK, recentRes.getStatusCode());
        var sessions = (List<?>) recentRes.getBody().get("sessions");
        assertEquals(1, sessions.size(), "Expected exactly one completed session in history");

        // 7. Fetch latest session — should match the session we just completed
        ResponseEntity<Map> latestRes = rest.exchange(
                "/api/v1/workout-sessions/latest", HttpMethod.GET,
                authedRequest(token), Map.class);
        assertEquals(HttpStatus.OK, latestRes.getStatusCode());
        assertEquals(sessionId, latestRes.getBody().get("sessionId"));
        assertTrue((Boolean) latestRes.getBody().get("completed"));

        // 8. Verify X-Request-ID correlation header is returned on authenticated requests
        assertNotNull(latestRes.getHeaders().getFirst("X-Request-ID"),
                "Every response should echo back a correlation ID header");
    }

    private static EmbeddedPostgres startEmbeddedPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start embedded Postgres for integration test", e);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmbeddedPostgresConfig {
        @Bean
        DataSource dataSource() {
            return POSTGRES.getPostgresDatabase();
        }
    }
}


