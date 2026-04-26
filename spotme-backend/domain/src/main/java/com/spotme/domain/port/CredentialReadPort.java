package com.spotme.domain.port;

import com.spotme.domain.model.user.UserCredentials;

import java.util.Optional;

public interface CredentialReadPort {
    Optional<UserCredentials> findByEmail(String email);
    boolean existsByEmail(String email);
}

