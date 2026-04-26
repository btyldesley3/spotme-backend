package com.spotme.domain.port;

/**
 * Port for password hashing so the application layer stays free of BCrypt/Argon2 imports.
 */
public interface PasswordHashPort {
    String hash(String rawPassword);
    boolean verify(String rawPassword, String storedHash);
}

