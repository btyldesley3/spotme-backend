package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserCredentials;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.CredentialReadPort;
import com.spotme.domain.port.PasswordHashPort;

import java.util.Locale;

/**
 * Authenticates a user by email and password.
 * Does NOT generate tokens — that is the responsibility of the transport layer (REST adapter).
 */
public class LoginUser {

    private final CredentialReadPort credentialRead;
    private final PasswordHashPort passwordHash;

    public record Command(String email, String rawPassword) {}

    public record Result(UserId userId, String email) {}

    public LoginUser(CredentialReadPort credentialRead, PasswordHashPort passwordHash) {
        this.credentialRead = credentialRead;
        this.passwordHash = passwordHash;
    }

    public Result handle(Command command) {
        var email = command.email() == null ? "" : command.email().toLowerCase(Locale.ROOT);
        var raw = command.rawPassword();

        UserCredentials credentials = credentialRead.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("invalid email or password"));

        if (!passwordHash.verify(raw, credentials.passwordHash())) {
            throw new AuthenticationFailedException("invalid email or password");
        }
        if (!credentials.alphaEligible()) {
            throw new AuthenticationFailedException("account is not approved for alpha access");
        }

        return new Result(credentials.userId(), credentials.email());
    }

    /** Opaque auth failure — avoid leaking whether the email exists or the password was wrong. */
    public static class AuthenticationFailedException extends RuntimeException {
        public AuthenticationFailedException(String message) {
            super(message);
        }
    }
}

