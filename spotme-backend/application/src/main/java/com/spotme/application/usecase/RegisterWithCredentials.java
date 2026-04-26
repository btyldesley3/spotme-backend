package com.spotme.application.usecase;

import com.spotme.domain.model.user.AlphaAccessPath;
import com.spotme.domain.model.user.ExperienceLevel;
import com.spotme.domain.model.user.RecoveryProfile;
import com.spotme.domain.model.user.TrainingGoal;
import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserCredentials;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.AlphaAccessPort;
import com.spotme.domain.port.CredentialReadPort;
import com.spotme.domain.port.CredentialWritePort;
import com.spotme.domain.port.PasswordHashPort;
import com.spotme.domain.port.UserWritePort;

import java.util.Locale;

/**
 * Registers a new user, enforcing the alpha-access gate (invite code OR email allowlist).
 * On success, persists both the User domain aggregate and the UserCredentials record.
 */
public class RegisterWithCredentials {

    private final UserWritePort userWrite;
    private final CredentialReadPort credentialRead;
    private final CredentialWritePort credentialWrite;
    private final AlphaAccessPort alphaAccess;
    private final PasswordHashPort passwordHash;

    public record Command(
            String email,
            String rawPassword,
            String inviteCode,        // nullable — allowlist is the fallback
            String experienceLevel,
            String trainingGoal,
            int baselineSleepHours,
            int stressSensitivity
    ) {}

    public record Result(UserId userId, String email) {}

    public RegisterWithCredentials(
            UserWritePort userWrite,
            CredentialReadPort credentialRead,
            CredentialWritePort credentialWrite,
            AlphaAccessPort alphaAccess,
            PasswordHashPort passwordHash
    ) {
        this.userWrite = userWrite;
        this.credentialRead = credentialRead;
        this.credentialWrite = credentialWrite;
        this.alphaAccess = alphaAccess;
        this.passwordHash = passwordHash;
    }

    public Result handle(Command command) {
        var email = requireNonBlank(command.email(), "email").toLowerCase(Locale.ROOT);
        var rawPassword = requireNonBlank(command.rawPassword(), "rawPassword");

        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException("password must be at least 8 characters");
        }
        if (credentialRead.existsByEmail(email)) {
            throw new IllegalArgumentException("email already registered");
        }

        var accessPath = resolveAlphaAccess(email, command.inviteCode());
        if (accessPath == null) {
            throw new AlphaAccessDeniedException(
                    "Registration is restricted to alpha testers. " +
                    "Please provide a valid invite code or contact support.");
        }

        var user = new User(
                UserId.random(),
                ExperienceLevel.valueOf(requireNonBlank(command.experienceLevel(), "experienceLevel").toUpperCase(Locale.ROOT)),
                TrainingGoal.valueOf(requireNonBlank(command.trainingGoal(), "trainingGoal").toUpperCase(Locale.ROOT)),
                new RecoveryProfile(command.baselineSleepHours(), command.stressSensitivity())
        );
        userWrite.save(user);

        var credentials = new UserCredentials(
                user.id(),
                email,
                passwordHash.hash(rawPassword),
                true,
                accessPath
        );
        credentialWrite.save(credentials);

        return new Result(user.id(), email);
    }

    private AlphaAccessPath resolveAlphaAccess(String email, String inviteCode) {
        if (inviteCode != null && !inviteCode.isBlank()) {
            if (alphaAccess.validateAndConsumeInviteCode(inviteCode)) {
                return AlphaAccessPath.INVITE_CODE;
            }
        }
        if (alphaAccess.isEmailAllowlisted(email)) {
            return AlphaAccessPath.EMAIL_ALLOWLIST;
        }
        return null;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    /** Thrown when neither invite code nor email allowlist grants alpha access. */
    public static class AlphaAccessDeniedException extends RuntimeException {
        public AlphaAccessDeniedException(String message) {
            super(message);
        }
    }
}

