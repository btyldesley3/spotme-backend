# SpotMe Backend

SpotMe is an adaptive training backend built with Java 21, Spring Boot 3.4, gRPC, and PostgreSQL.

The MVP flow currently supported is:
1. register a user
2. start a workout session
3. log sets
4. complete the session with recovery feedback (DOMS/sleep)
5. get the next recommendation from the progression engine

## Current Capabilities (MVP)

- Adaptive recommendation flow driven by `ProgressionEngine` in `domain/`.
- User registration and profile retrieval.
- Workout session lifecycle: start, log sets, complete, read latest/recent sessions.
- Postgres-backed persistence adapters for users, sessions, sets, and stored prescriptions.
- Flyway migrations for MVP schema and workout templates.
- gRPC API as primary transport, with REST endpoints acting as a facade over gRPC.
- End-to-end gRPC integration test for the adaptive flow in `app/src/test/java/com/spotme/AdaptiveTrainingFlowGrpcIntegrationTest.java`.

## Architecture

SpotMe uses a hexagonal, layered module layout:

```
domain -> application -> adapters -> app
```

- `domain/`: pure business logic, value objects, aggregates, ports, progression rules.
- `application/`: use cases (`RegisterUser`, `StartWorkoutSession`, `LogSet`, `CompleteWorkoutSession`, `ComputeNextPrescription`, etc.).
- `adapters/in.grpc/`: gRPC service implementation (`PlanGrpcService`).
- `adapters/in.rest/`: REST controller (`PlanRestController`) that calls a gRPC stub.
- `adapters/out.persistence/`: JPA entities/repositories and Postgres adapters.
- `adapters/out.rules/`: classpath JSON rules loader (`rules/v1.0.0.json`).
- `app/`: Spring Boot assembly and wiring.

Modules are declared in the root `pom.xml`.

## Tech Stack

- Java 21
- Spring Boot 3.4.1
- gRPC 1.66.0 + Protobuf 3.25.3
- PostgreSQL + Spring Data JPA
- Flyway migrations
- MapStruct (persistence mapping)
- JUnit 5 + Spring Boot Test + embedded Postgres (`io.zonky.test:embedded-postgres`)

## Running Locally

### Prerequisites

- JDK 21
- Maven wrapper (`mvnw` / `mvnw.cmd`)
- PostgreSQL (or Docker)

### Option A: Run with local Postgres

Set environment variables as needed (defaults shown in `app/src/main/resources/application.yaml`):

- `SPRING_DATASOURCE_URL` (default `jdbc:postgresql://localhost:5432/spotme`)
- `SPRING_DATASOURCE_USERNAME` (default `spotme`)
- `SPRING_DATASOURCE_PASSWORD` (default `spotme`)
- `SPRING_FLYWAY_ENABLED` (default `true`)

Build and run:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd -pl app spring-boot:run
```

### Option B: Run with Docker Compose

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Notes:
- `docker-compose.yaml` exposes REST on `8080` and gRPC on `9090`.
- `.env.example` currently sets `SPRING_FLYWAY_ENABLED=false`; enable it in `.env` if you want migrations applied automatically in Docker.

## API Surfaces

### gRPC (`PlanService`)

Defined in `proto/src/main/proto/plan/v1/plan.proto`:

- `RegisterUser`
- `GetUserProfile`
- `StartWorkoutSession`
- `LogSet`
- `CompleteWorkoutSession`
- `Recommend`
- `GetLatestWorkoutSession`
- `ListRecentWorkoutSessions`

Default gRPC server port: `9090` (`app/src/main/resources/application.yaml`).

### REST facade

Implemented in `adapters/in.rest/src/main/java/com/spotme/adapters/in/rest/PlanRestController.java`:

- `POST /api/v1/users`
- `GET /api/v1/users/{userId}`
- `POST /api/v1/workout-sessions/start`
- `POST /api/v1/workout-sessions/{sessionId}/sets`
- `POST /api/v1/workout-sessions/{sessionId}/complete`
- `POST /api/v1/recommendations`
- `GET /api/v1/workout-sessions/latest?userId=...`
- `GET /api/v1/workout-sessions/recent?userId=...&limit=...`

## Database & Migrations

Flyway scripts live in `adapters/out.persistence/src/main/resources/db/migration/`:

- `V1__create_mvp_schema.sql`
  - `users`
  - `workout_sessions`
  - `workout_sets`
  - `prescriptions`
- `V2__create_workout_templates.sql`
  - `workouts`

The app defaults to `spring.jpa.hibernate.ddl-auto=none` and relies on migrations/schema management.

## Testing

Run all tests:

```powershell
.\mvnw.cmd test
```

Run only the integration flow test in `app/`:

```powershell
.\mvnw.cmd -pl app -Dtest=AdaptiveTrainingFlowGrpcIntegrationTest test
```

The integration flow verifies register -> log workout -> complete -> recommend over gRPC with an embedded Postgres datasource.

## Key Files

- `domain/src/main/java/com/spotme/domain/rules/ProgressionEngine.java`
- `application/src/main/java/com/spotme/application/usecase/ComputeNextPrescription.java`
- `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/PlanGrpcService.java`
- `adapters/in.rest/src/main/java/com/spotme/adapters/in/rest/PlanRestController.java`
- `adapters/out.persistence/src/main/java/com/spotme/adapters/out/persistence/PostgresUserAdapter.java`
- `adapters/out.persistence/src/main/java/com/spotme/adapters/out/persistence/PostgresWorkoutAdapter.java`
- `app/src/main/java/com/spotme/config/UseCaseWiringConfig.java`

## Near-Term Next Steps

- Expand exercise/workout template CRUD use cases through gRPC.
- Harden validation and error mapping across transports.
- Add more integration tests for recent/latest session retrieval and error paths.
- Wire cache adapter where it provides measurable value.

