package com.spotme.adapters.in.rest.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static String base64Key(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generatesAndValidatesAccessToken() {
        var service = new JwtService(base64Key("0123456789abcdef0123456789abcdef"), 15, 7);

        var token = service.generateAccessToken("user-123", "user@spotme.dev");

        assertEquals("user-123", service.extractUserId(token));
    }

    @Test
    void rejectsBlankSecret() {
        var ex = assertThrows(IllegalStateException.class, () -> new JwtService("", 15, 7));
        assertEquals("spotme.jwt.secret must be configured as a base64-encoded key", ex.getMessage());
    }

    @Test
    void rejectsInvalidBase64Secret() {
        var ex = assertThrows(IllegalStateException.class, () -> new JwtService("not-base64!!!", 15, 7));
        assertEquals("spotme.jwt.secret must be valid base64", ex.getMessage());
    }

    @Test
    void rejectsTooShortSecret() {
        var tooShort = base64Key("short-key");

        var ex = assertThrows(IllegalStateException.class, () -> new JwtService(tooShort, 15, 7));
        assertEquals("spotme.jwt.secret must decode to at least 32 bytes (256-bit key)", ex.getMessage());
    }
}

