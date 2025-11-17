package com.spotme.domain.model.program.block;

import com.spotme.domain.model.program.TrainingWeek;

import java.util.List;
import java.util.Objects;

public class TrainingBlock {

    private final BlockId id;
    private final List<TrainingWeek> weeks;
    private final BlockType type; // e.g., ACCUMULATION, DELOAD

    public TrainingBlock(BlockId id, List<TrainingWeek> weeks, BlockType type) {
        this.id = Objects.requireNonNull(id);
        this.weeks = List.copyOf(weeks);
        this.type = type;
    }

    public BlockId getId() {
        return id;
    }

    public List<TrainingWeek> getWeeks() {
        return weeks;
    }

    public BlockType getType() {
        return type;
    }

}
