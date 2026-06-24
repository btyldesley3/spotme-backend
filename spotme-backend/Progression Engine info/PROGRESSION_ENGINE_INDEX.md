# Progression Engine Documentation Index

This folder contains design and verification material for SpotMe's adaptive progression engine.

## Read order

1. `PROGRESSION_ENGINE_QUICK_REF.md` - fast operational cheat sheet
2. `PROGRESSION_ENGINE_DESIGN.md` - detailed decision logic and data contracts
3. `PROGRESSION_ENGINE_DIAGRAMS.md` - architecture and flow diagrams
4. `VERIFICATION_CHECKLIST.md` - test/validation checklist before release

## System context

The progression engine is domain logic and is transport-agnostic.

- Domain computation: `domain/.../ProgressionEngine.java`
- Application orchestration: `ComputeNextPrescription`
- Inbound transport: gRPC `PlanService/Recommend`

## Inputs consumed

- last top set weight and reps
- last RPE
- DOMS and sleep quality feedback
- modality key and rules version

## Outputs produced

- one or more set prescriptions
- prescribed weight, reps, and backoff markers

## Key constraints

- deterministic behavior for identical input
- explicit fallback and validation errors
- rule versioning controlled by policy JSON in `adapters/out.rules`

## Related files in codebase

- `domain/src/main/java/com/spotme/domain/rules/ProgressionEngine.java`
- `application/src/main/java/com/spotme/application/usecase/ComputeNextPrescription.java`
- `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/PlanGrpcService.java`
- `proto/src/main/proto/plan/v1/plan.proto`
