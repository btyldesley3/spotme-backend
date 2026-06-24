# AGENTS.md – SpotMe Backend Development Guide

## Project Overview

**SpotMe** is an adaptive gym training companion built on **Java 21** with **Spring Boot 3.4** and **Domain-Driven Design (DDD)** principles. The backend is gRPC-native for MVP delivery and dynamically adjusts workouts based on user feedback (RPE, DOMS, sleep quality) via a rules-based progression engine.

**Primary Transport:** gRPC on port `9090`  
**Contract:** `proto/src/main/proto/plan/v1/plan.proto`  
**Services:** `AuthService`, `PlanService`  
**Database:** PostgreSQL with Flyway migrations  

## Layering and dependency flow

```text
domain/ (pure model + ports)
  <- application/ (use cases)
  <- adapters/ (in.grpc + out.*)
  <- app/ (Spring Boot assembly)
```

Rules:

- `domain` must not import Spring or adapter packages
- `application` depends only on `domain`
- adapters implement domain ports or call application use cases
- app module wires concrete runtime beans

## Module map

- `domain/`: aggregates, value objects, domain ports, progression logic
- `application/`: orchestration use cases (`ComputeNextPrescription`, `LoginUser`, etc.)
- `proto/`: protobuf files and generated stubs
- `adapters/in.grpc/`: gRPC endpoint handlers and auth interceptor
- `adapters/out.persistence/`: Postgres adapters, JPA entities/repos
- `adapters/out.rules/`: JSON policy loading
- `adapters/out.cache/`: optional cache extension point
- `app/`: bootstrapping and configuration

## gRPC services

### `AuthService`

- `RegisterCredentials`
- `Login`
- `RefreshToken`
- `Logout`
- `LogoutAll`

### `PlanService`

- `GetUserProfile`
- `StartWorkoutSession`
- `LogSet`
- `CompleteWorkoutSession`
- `Recommend`
- `GetLatestWorkoutSession`
- `ListRecentWorkoutSessions`

`PlanService/RegisterUser` is retained only as a deprecated RPC and intentionally blocked.

## Security boundaries

- JWT metadata key: `authorization`
- Prefix: `Bearer `
- Global interceptor: `GrpcJwtAuthInterceptor`
- Public auth RPCs (`RegisterCredentials`, `Login`, `RefreshToken`) bypass token requirement
- All other methods require valid JWT and matching `userId` ownership

## Core engine references

- Progression engine: `domain/src/main/java/com/spotme/domain/rules/ProgressionEngine.java`
- Recommendation orchestration: `application/src/main/java/com/spotme/application/usecase/ComputeNextPrescription.java`
- gRPC mapping: `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/PlanGrpcService.java`

## Development workflow

Build all:

```powershell
.\mvnw.cmd clean install
```

Run app:

```powershell
.\mvnw.cmd -pl app spring-boot:run
```

Run all tests:

```powershell
.\mvnw.cmd test
```

Run gRPC integration flow:

```powershell
.\mvnw.cmd -pl app -Dtest=AdaptiveTrainingFlowGrpcIntegrationTest test
```

Regenerate proto stubs:

```powershell
.\mvnw.cmd -pl proto clean compile
```

## Common implementation patterns

### Add a new capability

1. Update proto contract in `proto/src/main/proto/plan/v1/plan.proto`
2. Regenerate stubs
3. Add/extend application use case
4. Implement gRPC handler mapping in `adapters/in.grpc`
5. Add tests (unit + integration where appropriate)

### Add a new persistence-backed aggregate

1. Add domain model and ports
2. Add use cases
3. Add JPA entities/repos/mappers in `adapters/out.persistence`
4. Add Flyway migration
5. Add domain and integration tests

## Do and don't

Do:

- keep domain logic pure and deterministic
- map exceptions to explicit gRPC statuses
- prefer constructor injection
- validate contracts and UUID parsing at transport boundaries

Don't:

- place business logic in gRPC handlers
- return persistence entities across layers
- import adapters into domain/application
- modify generated proto classes by hand

## Current MVP focus

- Harden auth/session behavior under retries and token rotation
- Expand workout/template workflows
- Strengthen test coverage for failure paths
- Keep proto contract stable for mobile alpha clients
