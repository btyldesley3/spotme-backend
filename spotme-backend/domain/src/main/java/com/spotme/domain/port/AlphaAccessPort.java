package com.spotme.domain.port;

/**
 * Port for enforcing the alpha-access gate at registration time.
 * Invite codes are validated and consumed atomically to prevent reuse.
 */
public interface AlphaAccessPort {

    /**
     * Returns true if the raw invite code is valid and available (active, not exhausted, not expired).
     * If valid, increments the usage counter so the code cannot be over-used.
     */
    boolean validateAndConsumeInviteCode(String rawCode);

    /** Returns true if the given email is on the active alpha allowlist. */
    boolean isEmailAllowlisted(String email);
}

