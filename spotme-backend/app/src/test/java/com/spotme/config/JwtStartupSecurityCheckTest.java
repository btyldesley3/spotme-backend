package com.spotme.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtStartupSecurityCheckTest {

    private static String base64Key(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void allowsMissingSecretInTestProfile() {
        var environment = new MockEnvironment().withProperty("spring.profiles.active", "test");
        var check = new JwtStartupSecurityCheck(environment);

        assertDoesNotThrow(() -> check.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void failsWhenSecretMissingOutsideTestProfile() {
        var environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        var check = new JwtStartupSecurityCheck(environment);

        var ex = assertThrows(IllegalStateException.class,
                () -> check.run(new DefaultApplicationArguments(new String[0])));
        assertEquals(
                "JWT secret is required outside test profile. Set spotme.jwt.secret (for example via SPOTME_JWT_SECRET_BASE64).",
                ex.getMessage()
        );
    }

    @Test
    void failsWhenTestSecretIsUsedOutsideTestProfile() {
        var environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod")
                .withProperty("spotme.jwt.secret", JwtStartupSecurityCheck.TEST_PROFILE_SECRET);
        var check = new JwtStartupSecurityCheck(environment);

        var ex = assertThrows(IllegalStateException.class,
                () -> check.run(new DefaultApplicationArguments(new String[0])));
        assertEquals(
                "Refusing to start with test JWT secret outside test profile. Configure a dedicated production secret.",
                ex.getMessage()
        );
    }

    @Test
    void failsWhenSecretIsTooShortOutsideTestProfile() {
        var environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod")
                .withProperty("spotme.jwt.secret", base64Key("short-key"));
        var check = new JwtStartupSecurityCheck(environment);

        var ex = assertThrows(IllegalStateException.class,
                () -> check.run(new DefaultApplicationArguments(new String[0])));
        assertEquals("spotme.jwt.secret must decode to at least 32 bytes (256-bit key)", ex.getMessage());
    }

    @Test
    void allowsStrongSecretOutsideTestProfile() {
        var environment = new MockEnvironment()
                .withProperty("spring.profiles.active", "prod")
                .withProperty("spotme.jwt.secret", base64Key("0123456789abcdef0123456789abcdef"));
        var check = new JwtStartupSecurityCheck(environment);

        assertDoesNotThrow(() -> check.run(new DefaultApplicationArguments(new String[0])));
    }
}

