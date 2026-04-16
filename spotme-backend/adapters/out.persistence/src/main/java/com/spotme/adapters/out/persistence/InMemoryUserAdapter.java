package com.spotme.adapters.out.persistence;

import com.spotme.domain.model.user.User;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.UserReadPort;
import com.spotme.domain.port.UserWritePort;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserAdapter implements UserReadPort, UserWritePort {

    private final Map<UserId, User> users = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findById(UserId userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public void save(User user) {
        users.put(user.id(), user);
    }
}

