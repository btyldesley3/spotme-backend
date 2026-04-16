package com.spotme.application.usecase;

import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;

import java.util.NoSuchElementException;
import java.util.UUID;

final class UserExistenceGuard {

    private UserExistenceGuard() {
    }

    static UserId requireExistingUser(String userId, UserReadPort users) {
        var parsedUserId = new UserId(UUID.fromString(userId));
        users.findById(parsedUserId).orElseThrow(() -> new NoSuchElementException("User not found"));
        return parsedUserId;
    }
}

