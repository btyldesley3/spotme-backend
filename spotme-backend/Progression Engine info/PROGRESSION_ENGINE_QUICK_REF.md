# Progression Engine Quick Reference

## Purpose

Generate the next workout prescription from recent performance and recovery signals.

## Runtime entry point

- gRPC: `PlanService/Recommend`
- Use case: `ComputeNextPrescription`
- Domain engine: `ProgressionEngine`

## Required request fields

- `userId`
- `exerciseId`
- `rulesVersion` (defaultable by handler)
- `modalityKey` (defaultable by handler)

## Input signals used by the engine

- `lastTopSetWeightKg`
- `lastTopSetReps`
- `rpe`
- `doms`
- `sleepQuality`

## Output fields

For each prescribed set:

- `exerciseId`
- `order`
- `prescribedWeightKg`
- `prescribedReps`
- `isBackoff`

## High-level behavior

- low strain + good recovery -> progress load/reps
- moderate strain -> steady progression
- poor recovery/high strain -> conservative adjustment
- severe recovery risk -> mirror or reduce load based on policy

## Default status mapping (transport)

- invalid IDs/arguments -> `INVALID_ARGUMENT`
- no history/user/session -> `NOT_FOUND`
- auth/token issues -> `UNAUTHENTICATED`

## Operational checks

- keep rules JSON versioned and immutable
- verify recommendation payload is non-empty when history exists
- verify deterministic output for fixed input fixture
