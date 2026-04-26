package com.spotme.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Fails fast on insecure JWT startup configuration outside the test profile.
 */
@Component
public class JwtStartupSecurityCheck implements ApplicationRunner {

    /** Test-only key configured in application-test.yml; must never be used in non-test profiles. */
    static final String TEST_PROFILE_SECRET = "Zm9yLXRlc3RzLW9ubHktMzItYnl0ZS1qd3Qta2V5LTAwMDE=";

    private final Environment environment;

    public JwtStartupSecurityCheck(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (environment.acceptsProfiles(Profiles.of("test"))) {
            return;
        }

        var configuredSecret = environment.getProperty("spotme.jwt.secret");
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is required outside test profile. Set spotme.jwt.secret (for example via SPOTME_JWT_SECRET_BASE64)."
            );
        }
        if (TEST_PROFILE_SECRET.equals(configuredSecret)) {
            throw new IllegalStateException(
                    "Refusing to start with test JWT secret outside test profile. Configure a dedicated production secret."
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(configuredSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("spotme.jwt.secret must be valid base64", ex);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("spotme.jwt.secret must decode to at least 32 bytes (256-bit key)");
        }
    }
}

