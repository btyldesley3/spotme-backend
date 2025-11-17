package com.spotme.domain.model.program.block;

import java.util.UUID;

public record BlockId(UUID value) {

    public BlockId {
        if (value == null) {
            throw new IllegalArgumentException("BlockId value cannot be null.");
        }
    }

    public static BlockId generate() {
        return new BlockId(UUID.randomUUID());
    }

    public static BlockId fromString(String raw) {
        return new BlockId(UUID.fromString(raw));
    }
}
