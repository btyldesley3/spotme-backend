# SpotMe Backend

SpotMe is an adaptive training backend built with Java 21, Spring Boot 3.4, gRPC, and PostgreSQL.

The backend is now fully gRPC-native for MVP delivery. Both authentication and training workflows are exposed through protobuf contracts and gRPC services.

## MVP Flow

1. Register credentials (`AuthService/RegisterCredentials`)
2. Login (`AuthService/Login`) and receive JWT access + refresh tokens
3. Start a workout session (`PlanService/StartWorkoutSession`)
4. Log sets (`PlanService/LogSet`)
5. Complete session with recovery feedback (`PlanService/CompleteWorkoutSession`)
6. Request the next recommendation (`PlanService/Recommend`)
7. Read latest/recent sessions (`PlanService/GetLatestWorkoutSession`, `PlanService/ListRecentWorkoutSessions`)

## Architecture

SpotMe uses a hexagonal module layout with strict dependency flow:

```text
domain -> application -> adapters -> app
```

- `domain/`: core business model, rules, value objects, and ports
- `application/`: use cases orchestrating domain behavior
- `proto/`: protobuf contracts and generated Java stubs
- `adapters/in.grpc/`: inbound gRPC services (`AuthGrpcService`, `PlanGrpcService`)
- `adapters/out.persistence/`: Postgres adapters and JPA repositories
- `adapters/out.rules/`: rules policy loading from classpath JSON
- `adapters/out.cache/`: cache adapter module (optional)
- `app/`: Spring Boot assembly and runtime wiring

## API Contracts

Protos are defined in:

- `proto/src/main/proto/plan/v1/plan.proto`

Services:

- `AuthService`: register, login, refresh, logout, logout-all
- `PlanService`: workout lifecycle + recommendations + profile/session reads

## Running Locally

### Prerequisites

- JDK 21
- Docker (optional, for local infra)

### Build

```powershell
.\mvnw.cmd clean install
```

### Run app module

```powershell
.\mvnw.cmd -pl app spring-boot:run
```

Default ports/config:

- gRPC server: `9090` (see `app/src/main/resources/application.yaml`)
- JWT secret: `SPOTME_JWT_SECRET_BASE64` (required outside `test` profile)

## Testing

Run all tests:

```powershell
.\mvnw.cmd test
```

Run core gRPC integration flow:

```powershell
.\mvnw.cmd -pl app -Dtest=AdaptiveTrainingFlowGrpcIntegrationTest test
```

## Docker

`docker-compose.yaml` runs:

- `postgres` on `5432`
- `spotme-api` gRPC on `9090`

See `DOCKER_LOCAL.md` for full local Docker workflow.

## Key Files

- `app/src/main/java/com/spotme/SpotMeApplication.java`
- `app/src/main/java/com/spotme/config/UseCaseWiringConfig.java`
- `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/AuthGrpcService.java`
- `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/PlanGrpcService.java`
- `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/GrpcJwtAuthInterceptor.java`
- `domain/src/main/java/com/spotme/domain/rules/ProgressionEngine.java`

## Status

- gRPC-native auth and training flows are implemented
- Postgres persistence and Flyway migrations are active
- Domain progression engine is integrated through `ComputeNextPrescription`
- Integration test coverage includes authenticated gRPC happy-path scenarios
