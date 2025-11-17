package com.spotme.domain.model.program.exercise;

import java.util.UUID;

public record SetId(UUID value) {
    public SetId {
        if (value == null) {
            throw new IllegalArgumentException("SetId value cannot be null.");
        }
    }

    public static SetId generate() {
        return new SetId(UUID.randomUUID());
    }

}
