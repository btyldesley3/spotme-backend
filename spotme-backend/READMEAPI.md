# SpotMe gRPC API Guide

This guide documents the active API surface for SpotMe.

- Transport: gRPC only (MVP)
- Contract source: `proto/src/main/proto/plan/v1/plan.proto`
- Services: `AuthService`, `PlanService`

## 1) Service Overview

### `AuthService`

Public methods:

- `RegisterCredentials`
- `Login`
- `RefreshToken`

Authenticated methods:

- `Logout`
- `LogoutAll`

### `PlanService`

Authenticated methods:

- `GetUserProfile`
- `StartWorkoutSession`
- `LogSet`
- `CompleteWorkoutSession`
- `Recommend`
- `GetLatestWorkoutSession`
- `ListRecentWorkoutSessions`

Deprecated method:

- `RegisterUser` (kept for compatibility; returns `PERMISSION_DENIED`)

## 2) Authentication Model

SpotMe uses Bearer JWT in gRPC metadata:

- Header key: `authorization`
- Value: `Bearer <access_token>`

JWT behavior:

- Access tokens are short-lived
- Refresh tokens are opaque values stored as SHA-256 hashes server-side
- `RefreshToken` rotates token state by consuming an existing refresh token and issuing a new pair

The global auth interceptor in `adapters/in.grpc` enforces:

- Missing/invalid token -> `UNAUTHENTICATED`
- Token user mismatch vs request `user_id` -> `PERMISSION_DENIED`

## 3) Error Semantics

Common gRPC status mappings used by handlers:

- `INVALID_ARGUMENT`: validation or malformed request fields
- `NOT_FOUND`: unknown user/session/history
- `UNAUTHENTICATED`: missing/invalid/expired JWT or refresh token invalid
- `PERMISSION_DENIED`: disallowed operation (for example deprecated RPC or user-id mismatch)

## 4) Core Workflow (Happy Path)

1. `RegisterCredentials`
2. `Login` -> receive `access_token` + `refresh_token`
3. Attach access token in metadata
4. `StartWorkoutSession`
5. `LogSet` (1..n)
6. `CompleteWorkoutSession` with recovery inputs
7. `Recommend`
8. Optionally query `GetLatestWorkoutSession` / `ListRecentWorkoutSessions`

## 5) Local Testing Tips

You can use:

- Postman gRPC client
- `grpcurl`
- IntelliJ gRPC plugin

Example reflection check:

```powershell
grpcurl -plaintext localhost:9090 list
```

If reflection is enabled, you should see `com.spotme.proto.plan.v1.AuthService` and `com.spotme.proto.plan.v1.PlanService`.

## 6) Contract Evolution Rules

- Add new capabilities to proto first
- Keep existing field numbers stable
- Add new fields with new numbers only
- Avoid breaking RPC signature changes in alpha
- Mark deprecated RPCs with comments before removal

## 7) Source Pointers

- Contract: `proto/src/main/proto/plan/v1/plan.proto`
- Auth handler: `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/AuthGrpcService.java`
- Plan handler: `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/PlanGrpcService.java`
- JWT interception: `adapters/in.grpc/src/main/java/com/spotme/adapters/in/grpc/GrpcJwtAuthInterceptor.java`
- Integration test: `app/src/test/java/com/spotme/AdaptiveTrainingFlowGrpcIntegrationTest.java`
