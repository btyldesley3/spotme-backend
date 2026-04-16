package com.spotme.domain.port;

import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;

import java.util.Optional;

public interface UserReadPort {
    Optional<User> findById(UserId userId);
}

