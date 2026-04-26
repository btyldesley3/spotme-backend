package com.spotme.domain.model.user;

import java.util.Objects;

/**
 * Represents the authentication credentials and alpha access state for a registered user.
 * Kept separate from {@link User} so that identity/auth concerns stay out of the domain aggregate.
 */
public class UserCredentials {

    private final UserId userId;
    private final String email;
    private final String passwordHash;
    private final boolean alphaEligible;
    private final AlphaAccessPath alphaAccessPath;

    public UserCredentials(
            UserId userId,
            String email,
            String passwordHash,
            boolean alphaEligible,
            AlphaAccessPath alphaAccessPath
    ) {
        this.userId = Objects.requireNonNull(userId, "userId required");
        this.email = Objects.requireNonNull(email, "email required");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash required");
        this.alphaEligible = alphaEligible;
        this.alphaAccessPath = alphaAccessPath;
    }

    public UserId userId() { return userId; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public boolean alphaEligible() { return alphaEligible; }
    public AlphaAccessPath alphaAccessPath() { return alphaAccessPath; }
}

