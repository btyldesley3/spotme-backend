# SpotMe API Guide

This document describes the **current API setup** in the SpotMe backend and the **intended transport strategy** as the product moves toward a fuller MVP.

The short version:

- **gRPC is the primary application contract**.
- **REST is the web-facing facade** for clients that need HTTP/JSON.
- Both transports should reach the **same application use cases** and **same domain logic**.

---

## 1. API Strategy

### Intended direction

SpotMe is designed around a clear transport boundary:

- **Use gRPC internally** for backend-to-backend communication.
- **Use REST externally** when calls are exposed to the web, browser-based clients, or systems that are naturally HTTP/JSON oriented.

This matches the current architecture goals:

- gRPC provides a strongly-typed contract, efficient serialization, and first-class code generation.
- REST remains the most practical interface for public web consumption because it is HTTP-native and easier to integrate with browsers, API gateways, Postman, and third-party systems.

### Design principle

Transport should not own business logic.

Instead:

1. **`proto/` defines the canonical service contract**.
2. **`adapters/in.grpc/` implements that contract** and calls application use cases.
3. **`adapters/in.rest/` acts as a facade** that translates HTTP/JSON requests into gRPC calls.
4. **`application/` and `domain/` remain transport-agnostic**.

That keeps the adaptive training engine independent from whether the caller used gRPC or REST.

---

## 2. Current Implementation

### Transport layout today

The current codebase exposes both transports from the same backend:

- **gRPC server** for the primary API surface
- **REST controller** for HTTP/JSON access
- **PostgreSQL-backed persistence** behind the same use cases

### Current ports and wiring

From `app/src/main/resources/application.yaml`:

- gRPC server port: **`9090`**
- REST runs through Spring Boot’s web stack (commonly `8080` when started with default Boot settings or Docker mapping)

From `adapters/in.rest/src/main/java/com/spotme/adapters/in/rest/config/RestToGrpcConfig.java`:

- the REST adapter creates a gRPC client channel to:
  - host: `localhost` by default
  - port: `9090` by default
  - plaintext: `true` by default

So today the REST layer is not a separate business API implementation. It is a **translation layer over the gRPC service**, currently using an in-process/local-network gRPC client configuration from the same backend deployment.

---

## 3. Current gRPC API

The primary contract is defined in:

- `proto/src/main/proto/plan/v1/plan.proto`

Current `PlanService` RPCs:

| RPC | Purpose |
|---|---|
| `RegisterUser` | Create a new user profile |
| `GetUserProfile` | Fetch a user profile |
| `StartWorkoutSession` | Start a new workout session |
| `LogSet` | Log a set into an active session |
| `CompleteWorkoutSession` | Finalize a session and record recovery signals |
| `Recommend` | Generate the next prescription using the adaptive engine |
| `GetLatestWorkoutSession` | Fetch the latest session for a user |
| `ListRecentWorkoutSessions` | List recent sessions for a user |

### Why gRPC is the core contract

gRPC is a good fit for SpotMe because the backend is centered on structured, stateful workflows:

- user registration
- session lifecycle management
- set logging
- recommendation generation
- session history retrieval

These operations benefit from:

- generated clients from `.proto`
- strongly-typed request/response schemas
- fewer hand-written DTO contracts across services
- a single source of truth for API evolution

### Current gRPC implementation

The gRPC adapter lives in:

- `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/PlanGrpcService.java`

`PlanGrpcService` currently:

- receives protobuf requests
- converts them into application-layer commands
- invokes use cases such as:
  - `RegisterUser`
  - `GetUserProfile`
  - `StartWorkoutSession`
  - `LogSet`
  - `CompleteWorkoutSession`
  - `ComputeNextPrescription`
  - `GetLatestWorkoutSession`
  - `ListRecentWorkoutSessions`
- translates some domain/application exceptions into gRPC status codes such as:
  - `INVALID_ARGUMENT`
  - `NOT_FOUND`

This is the preferred place for transport-specific error mapping on the gRPC side.

---

## 4. Current REST API

The REST facade lives in:

- `adapters/in.rest/src/main/java/com/spotme/adapters/in/rest/PlanRestController.java`

Current REST endpoints:

| REST endpoint | Delegates to gRPC |
|---|---|
| `GET /api/v1/users/{userId}` | `GetUserProfile` |
| `POST /api/v1/workout-sessions/start` | `StartWorkoutSession` |
| `POST /api/v1/workout-sessions/{sessionId}/sets` | `LogSet` |
| `POST /api/v1/workout-sessions/{sessionId}/complete` | `CompleteWorkoutSession` |
| `POST /api/v1/recommendations` | `Recommend` |
| `GET /api/v1/workout-sessions/latest` | `GetLatestWorkoutSession` |
| `GET /api/v1/workout-sessions/recent?limit=...` | `ListRecentWorkoutSessions` |

### Auth endpoints (REST)

Auth is exposed under `adapters/in.rest/src/main/java/com/spotme/adapters/in/rest/AuthRestController.java`:

| REST endpoint | Auth required | Purpose | Success | Common failure |
|---|---|---|---|---|
| `POST /api/auth/register` | No | Register a new alpha-eligible user (invite code OR allowlist) | `201 Created` | `403` if not eligible |
| `POST /api/auth/login` | No | Authenticate email/password and issue access + refresh tokens | `200 OK` | `401` invalid credentials |
| `POST /api/auth/refresh` | No | Rotate refresh token and issue new access + refresh tokens | `200 OK` | `401` invalid/expired refresh token |
| `POST /api/auth/logout` | Yes (Bearer JWT) | Revoke all outstanding refresh tokens for current user | `204 No Content` | `401` missing/invalid JWT |
| `POST /api/auth/logout-all` | Yes (Bearer JWT) | Explicit alias of logout semantics; revokes all refresh tokens for current user | `204 No Content` | `401` missing/invalid JWT |

`/api/auth/logout` and `/api/auth/logout-all` are currently equivalent for client clarity and forward compatibility.

### What REST is doing today

The REST adapter currently:

- accepts JSON requests
- validates request DTOs with Spring validation annotations
- derives user identity from JWT for protected routes (instead of trusting request-supplied user IDs)
- builds protobuf requests
- calls a local gRPC blocking stub
- maps protobuf responses back into JSON DTOs

This means REST is currently a **consumer of the gRPC API**, not a parallel hand-built API stack.

That is aligned with the intended architecture.

---

## 5. Recommended Usage Model

### Use gRPC when

Prefer gRPC for:

- internal service-to-service communication
- future background workers or event-driven processors that need direct backend access
- JVM or server-side consumers that can use generated protobuf clients
- high-confidence contract evolution across internal systems

Examples:

- a future coaching/orchestration service consuming recommendations
- an internal scheduling service creating workout sessions
- a backend integration service reading session history

### Use REST when

Prefer REST for:

- browser-based clients
- public/mobile/web-facing integrations
- API gateway exposure
- debugging and manual testing via HTTP tools
- integrations where HTTP/JSON is the expected contract

Examples:

- a web app registering a user
- a frontend submitting completed workout data
- a public-facing recommendation endpoint exposed behind a gateway

---

## 6. Request Flow

### Current gRPC flow

```text
Client -> PlanService (gRPC) -> application use case -> domain -> persistence/rules adapters
```

### Current REST flow

```text
Client -> REST controller -> gRPC stub -> PlanService (gRPC) -> application use case -> domain -> persistence/rules adapters
```

### Why keep REST as a facade

Keeping REST as a facade has a few advantages:

- one primary contract to evolve
- less duplicated orchestration logic
- less risk that REST and gRPC behavior drift apart
- easier parity between internal and external flows

The tradeoff is an extra translation hop, which is acceptable for the MVP and keeps the architecture simple.

---

## 7. Adaptive Training Flow Through the API

The core MVP workflow is already represented across both transports:

1. **Register user**
2. **Start workout session**
3. **Log workout sets**
4. **Complete workout session** with recovery inputs like DOMS and sleep quality
5. **Request recommendation** for the next prescription

On the recommendation step, the system ultimately flows through the adaptive training engine in `domain/`, with the current contract exposed via:

- gRPC: `Recommend`
- REST: `POST /api/v1/recommendations`

This is the key API path for the MVP.

---

## 8. File Map

Key transport files:

- `proto/src/main/proto/plan/v1/plan.proto`
- `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/PlanGrpcService.java`
- `adapters/in.rest/src/main/java/com/spotme/adapters/in/rest/PlanRestController.java`
- `adapters/in.rest/src/main/java/com/spotme/adapters/in/rest/config/RestToGrpcConfig.java`
- `app/src/main/resources/application.yaml`

Key supporting layers:

- `application/src/main/java/com/spotme/application/usecase/`
- `domain/src/main/java/com/spotme/domain/`
- `adapters/out.persistence/`
- `adapters/out.rules/`

---

## 9. Near-Term API Priorities

Recommended next API steps for the MVP:

1. **Keep gRPC as the source of truth** for new workflow capabilities.
2. **Expose only the REST endpoints that are actually needed externally**.
3. **Harden error translation** so REST returns clean HTTP responses derived from gRPC/domain failures.
4. **Add more end-to-end integration coverage** for both success and failure flows.
5. **Continue tightening authentication/authorization boundaries** (JWT ownership checks are in place; expand as new endpoints are added).
6. **Expand workout/exercise template APIs** using the same contract-first approach.

---

## 10. Practical Rule of Thumb

If a new capability is being added:

- define or extend it in `plan.proto`
- implement it in `PlanGrpcService`
- wire it to `application/` use cases
- only then add a REST endpoint if it needs to be exposed over the web

In other words:

- **gRPC first for internal product contracts**
- **REST second for external delivery**

That is the intended API posture for SpotMe.


