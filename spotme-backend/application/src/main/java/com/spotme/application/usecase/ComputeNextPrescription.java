package com.spotme.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.spotme.domain.model.exercise.ExerciseId;
import com.spotme.domain.model.plan.Prescription;
import com.spotme.domain.model.user.UserId;
import com.spotme.domain.port.RulesConfigPort;
import com.spotme.domain.port.WorkoutReadPort;
import com.spotme.domain.port.WorkoutWritePort;
import com.spotme.domain.rules.ProgressionEngine;
import com.spotme.domain.rules.ProgressionPolicy;

import java.util.UUID;

public class ComputeNextPrescription {
    private final WorkoutReadPort read;
    private final WorkoutWritePort write;
    private final RulesConfigPort rules;
    private final ProgressionEngine engine = new ProgressionEngine();

    public record Command(
            String userId,
            String exerciseId,
            String rulesVersion,
            String modalityKey) { }

    public record Result(
            Prescription prescription) { }

    public ComputeNextPrescription(WorkoutReadPort read, WorkoutWritePort write, RulesConfigPort config) {
        this.read = read;
        this.write = write;
        this.rules = config;
    }

    public Result handle(Command command) {
        var userId = new UserId(UUID.fromString(command.userId()));
        var exerciseId = new ExerciseId(UUID.fromString(command.exerciseId()));

        var last = read.lastProgressionInput(userId, exerciseId).orElseThrow(); // 404 or domain error in adapter if missing
        JsonNode rulesJson = rules.loadRules(command.rulesVersion());
        var policy = ProgressionPolicy.fromJson(rulesJson, command.modalityKey());

        var prescription = engine.nextFor(exerciseId, last, policy);
        write.savePrescription(userId, prescription);
        return new Result(prescription);
    }
}
