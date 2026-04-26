package com.spotme.domain.port;

import com.spotme.domain.model.user.UserCredentials;

public interface CredentialWritePort {
    void save(UserCredentials credentials);
}

