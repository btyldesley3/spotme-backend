# SpotMe Alpha MVP Spec (gRPC-native)

## 1) Product goal

Deliver a mobile-usable alpha where users can authenticate, log training sessions, and receive adaptive recommendations through a single gRPC contract.

North-star outcome: a new user completes the full loop (register -> train -> recommend) and returns for next-session execution.

## 2) In-scope MVP

- gRPC-native auth (`AuthService`): register, login, refresh, logout, logout-all
- gRPC-native training (`PlanService`): profile, workout lifecycle, recommendation, session history
- JWT metadata auth enforcement for protected RPCs
- Alpha access gate: invite code or email allowlist
- Postgres persistence via adapters and Flyway migrations
- Integration coverage for authenticated happy path and key auth failures

## 3) Out-of-scope (alpha)

- REST facade
- Coach tooling and social features
- Third-party wearable integrations
- Billing/subscription flows
- Event streaming architecture changes

## 4) Architecture decisions (locked)

- Transport contract: `proto/src/main/proto/plan/v1/plan.proto`
- Inbound adapter: `adapters/in.grpc`
- Domain/application remain framework-agnostic and transport-agnostic
- Persistence through ports in `domain` and adapters in `adapters/out.persistence`

Rule: capability design starts in proto, then gRPC handler, then use-case/domain updates as needed.

## 5) Security model

- Access token in gRPC metadata: `authorization: Bearer <jwt>`
- Interceptor enforces JWT validity and request user ownership
- Refresh token stored as hash and consumed on refresh
- Startup guard blocks weak/missing JWT secret outside `test` profile

## 6) Happy-path flow

1. `AuthService/RegisterCredentials`
2. `AuthService/Login`
3. `PlanService/StartWorkoutSession`
4. `PlanService/LogSet` (one or more)
5. `PlanService/CompleteWorkoutSession`
6. `PlanService/Recommend`
7. `PlanService/GetLatestWorkoutSession` or `ListRecentWorkoutSessions`

## 7) Acceptance criteria

- User can register and login through gRPC
- Registration denied when not allowlisted and invite invalid
- Protected plan RPCs reject missing/invalid JWT (`UNAUTHENTICATED`)
- Cross-user spoofing is blocked (`PERMISSION_DENIED`)
- Full authenticated workout flow returns recommendation payload
- Flyway migrations bootstrap clean DB successfully
- Integration test verifies end-to-end gRPC flow on embedded Postgres

## 8) Delivery milestones

### M1: gRPC auth + protection

- Complete `AuthService` handler behavior
- Ensure interceptor allowlist/public methods are correct
- Verify refresh/logout token lifecycle

### M2: core flow hardening

- Expand edge-case validation around workout completion and recommendation
- Improve consistent gRPC status code mapping
- Add failure-mode tests

### M3: alpha readiness

- Contract freeze for mobile client
- Operational runbook for gRPC deployment and secrets
- Seed data + support checklist

## 9) Change control

Before adding scope, confirm:

- Improves core alpha loop directly
- Does not destabilize proto contract late in cycle
- Can be validated end-to-end this sprint
- Does not force new infrastructure beyond MVP needs

If not, defer to post-alpha backlog.

## 10) Post-alpha candidates

- richer template/exercise CRUD
- event-driven policy evolution
- advanced personalization and coaching insights
