package com.spotme.domain.port;

import com.spotme.domain.model.user.User;

public interface UserWritePort {
    void save(User user);
}

