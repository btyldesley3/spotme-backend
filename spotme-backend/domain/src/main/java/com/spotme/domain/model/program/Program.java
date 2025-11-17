package com.spotme.domain.model.program;

import com.spotme.domain.model.program.block.TrainingBlock;

import java.util.List;
import java.util.Objects;

public class Program {

    private final ProgramId id;
    private final List<TrainingBlock> blocks;

    public Program(ProgramId id, List<TrainingBlock> blocks) {
        this.id = Objects.requireNonNull(id);
        this.blocks = List.copyOf(blocks);
    }

    public ProgramId getId() {
        return id;
    }

    public List<TrainingBlock> getBlocks() {
        return blocks;
    }
}
