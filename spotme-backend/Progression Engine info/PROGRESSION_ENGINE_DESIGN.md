# Progression Engine Design

## 1) Objective

Provide safe, adaptive progression recommendations that balance overload and recovery.

## 2) Placement in architecture

- `domain`: pure progression decision logic
- `application`: retrieves historical signals and policies, then calls domain engine
- `adapters/in.grpc`: exposes recommendation via `PlanService/Recommend`

The engine itself is transport-independent.

## 3) Data flow

1. Client calls `Recommend` with user/exercise and policy selectors
2. Handler validates and maps request
3. Use case loads:
   - user existence
   - last progression input from workout history
   - rules policy JSON by version
4. Engine computes next prescription
5. Handler maps domain prescription to protobuf response

## 4) Inputs

### Performance signals

- top set weight
- top set reps
- RPE

### Recovery signals

- DOMS
- sleep quality

### Policy controls

- micro-load increments
- thresholds for low/optimal/high strain
- recovery weighting and safety caps

## 5) Decision principles

- progression is earned when performance and recovery support it
- recommendation should avoid abrupt jumps
- low confidence or missing recovery detail should bias conservative behavior
- severe recovery flags should prevent aggressive loading

## 6) Output model

A recommendation is a list of set prescriptions containing:

- exercise id
- set order
- prescribed reps
- prescribed load
- backoff indicator

## 7) Validation and error handling

- malformed IDs or unsupported arguments -> `INVALID_ARGUMENT`
- missing user/history -> `NOT_FOUND`
- auth failures before reaching use case -> `UNAUTHENTICATED`

## 8) Determinism and reproducibility

For identical inputs + identical rules version, output should remain deterministic.

Recommended practice:

- fixture-based unit tests for domain engine
- integration tests around gRPC mapping and persistence retrieval

## 9) Versioning strategy

Rules are externalized as versioned JSON. This allows policy evolution without breaking contract shape.

- old versions remain loadable during alpha compatibility windows
- new versions are rolled out explicitly via `rulesVersion`

## 10) Future extensions

- modality-specific fatigue accumulation models
- personalized progression coefficients per user profile
- rule explainability metadata in response payloads
