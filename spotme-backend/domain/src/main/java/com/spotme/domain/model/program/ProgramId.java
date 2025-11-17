package com.spotme.domain.model.program;

import java.util.UUID;

public record ProgramId(UUID value) {
    public ProgramId {
        if (value == null) {
            throw new IllegalArgumentException("ProgramId value cannot be null.");
        }
    }

    public static ProgramId generate() {
        return new ProgramId(UUID.randomUUID());
    }

    public static ProgramId fromString(String raw) {
        return new ProgramId(UUID.fromString(raw));
    }
}
