# Progression Engine Diagrams

## 1) Runtime architecture

```text
Client (mobile/web/backend)
        |
        | gRPC Recommend
        v
PlanService (adapters/in.grpc)
        |
        v
ComputeNextPrescription (application)
        |
        +--> UserReadPort (out.persistence)
        +--> WorkoutReadPort (out.persistence)
        +--> RulesConfigPort (out.rules)
        |
        v
ProgressionEngine (domain)
        |
        v
RecommendResponse (protobuf)
```

## 2) Auth-protected call path

```text
Incoming gRPC call
   |
GrpcJwtAuthInterceptor
   |-- verify Bearer token
   |-- verify userId ownership (when present)
   v
PlanGrpcService.recommend
   v
Application use case
   v
Domain engine
```

## 3) Decision flow (simplified)

```text
Load last progression input
   |
   +-- missing -> NOT_FOUND
   |
Evaluate strain + recovery
   |
   +-- high risk -> conservative branch
   +-- moderate -> steady branch
   +-- strong readiness -> progression branch
   |
Assemble set prescriptions
   |
Return recommendation
```

## 4) Rules versioning flow

```text
RecommendRequest.rulesVersion
        |
RulesConfigPort.load(version)
        |
        +-- unknown -> INVALID_ARGUMENT
        +-- found   -> policy object
        |
ProgressionEngine.nextFor(..., policy)
```

## 5) Test coverage map

```text
domain tests
  - deterministic progression behavior
  - edge cases and threshold transitions

adapter/unit tests
  - gRPC status mapping
  - request/response field mapping

app/integration tests
  - auth + workout + recommendation happy path
  - auth failure and protected endpoint checks
```
