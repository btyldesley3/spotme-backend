# SpotMe Alpha MVP One-Pager

## 1) Product Goal (Alpha)
Ship a mobile-usable SpotMe alpha where a user can securely sign up, log training sessions, and receive adaptive programming recommendations from the progression engine.

**North-star outcome:** users complete at least one full workout flow and receive a recommendation they can use in their next session.

## 2) MVP Scope (What we ship)
### In scope
- User account creation and login.
- JWT-protected API access for user-specific operations.
- Workout lifecycle:
  - start session
  - log sets
  - complete session with recovery feedback (DOMS, sleep quality)
  - fetch latest/recent sessions
- Recommendation generation from adaptive engine (`Recommend`).
- Postgres persistence with Flyway migrations.
- API availability through:
  - gRPC as canonical internal contract
  - REST facade for web/mobile clients

### Out of scope (Alpha)
- Social/community features.
- Advanced program builder and coach tooling.
- Wearables and third-party integrations.
- Payments/subscriptions.
- Broad analytics dashboards.

## 3) Target User Journey (Happy Path)
1. User registers.
2. User logs in and receives JWT.
3. User starts a workout session.
4. User logs one or more sets.
5. User completes session with recovery inputs.
6. User requests recommendation for next session.
7. User views latest/recent history.

This is the minimum lovability loop for alpha.

## 4) API + Architecture Decisions (Locked for Alpha)
- **Internal/service contract:** gRPC (`proto/src/main/proto/plan/v1/plan.proto`).
- **External/mobile/web contract:** REST facade (`adapters/in.rest/.../PlanRestController.java`).
- **Business logic location:** application/domain only (no business logic in transport adapters).
- **Persistence:** Postgres adapters + Flyway migrations.

**Rule:** new capability is designed in proto first, implemented in gRPC handler, then exposed via REST only if needed externally.

## 5) Security Decision (Alpha)
- Use JWT for REST authentication.
- Protect all user/workout/recommendation endpoints except auth/register.
- Keep internal gRPC trust boundary simple in alpha (single deployment).
- Use password hashing and never store plaintext credentials.

**Alpha access policy (locked)**
- Registration is allowed when either condition passes:
  - valid invite code
  - email is present on active alpha allowlist
- If both checks fail, registration is denied.
- Record which path granted access (`INVITE_CODE` or `EMAIL_ALLOWLIST`) for support/audit.
- Mark approved users as alpha-eligible and only allow alpha-eligible users to authenticate.

**JWT minimum for alpha**
- Subject: `userId`
- Expiration: short-lived access token
- Signing key from environment/config
- Include identity/authorization claims needed for alpha gating.

## 6) Acceptance Criteria (Definition of Done)
Alpha is ready when all are true:
- A new user can register and log in.
- Registration is denied when invite code is invalid and email is not allowlisted.
- Authenticated user can execute full flow: start -> log sets -> complete -> recommend.
- Recommendation response returns valid set prescription payload for supported modality/rules version.
- Unauthorized requests are rejected for protected routes.
- Flyway migrations bootstrap schema on clean DB.
- At least one end-to-end integration test covers authenticated full flow.
- Error responses are consistent and actionable (validation vs not found vs unauthorized).

## 7) Non-Functional Guardrails (Alpha-level)
- API contracts versioned and stable during alpha period.
- Basic observability:
  - request logging with correlation id
  - error rates for auth and recommend endpoints
- Reliability baseline:
  - no data loss for completed sessions
  - idempotent behavior expectations documented for mobile retries
- Performance target (alpha): acceptable interactive latency for core endpoints (monitor p95, tune post-alpha).

## 8) Delivery Plan (4-6 Weeks)
### Milestone 1: Auth + Protection (Week 1-2)
- Credential model + migration.
- Register/login endpoints.
- Invite code + email allowlist registration gates.
- JWT issuance/validation.
- Protect existing workout/recommendation endpoints.

### Milestone 2: Core Flow Hardening (Week 3-4)
- Validate workout lifecycle edge cases.
- Tighten REST<->gRPC error mapping.
- Expand integration tests for success and key failure paths.

### Milestone 3: Alpha Readiness (Week 5-6)
- Contract freeze for mobile client.
- Basic telemetry/monitoring dashboards.
- Launch checklist + seed/test data flow.

## 9) Change-Control Checklist (Prevent Scope Creep)
Before adding any new feature, confirm all are true:
- Does it directly improve the alpha happy path?
- Is it required for onboarding, logging, or recommendation trust?
- Can it be delivered without changing core API contracts late?
- Does it avoid adding new infrastructure not required for alpha?
- Can it be tested end-to-end this sprint?

If any answer is "no", defer to post-alpha backlog.

## 10) Post-Alpha Candidates (Not now)
- Refresh token rotation + session management hardening.
- Exercise/template CRUD expansion.
- Personalized onboarding and deeper progression policies.
- Community and coach-facing features.

