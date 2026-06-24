# Progression Engine Verification Checklist

Use this checklist before releasing progression-related changes.

## A) Contract and transport

- [ ] `plan.proto` updates are backward-compatible (no field renumbering)
- [ ] `PlanService/Recommend` request/response mappings are validated
- [ ] gRPC status codes are explicit for invalid/missing data cases
- [ ] protected call path requires JWT metadata

## B) Auth and ownership enforcement

- [ ] request rejected with `UNAUTHENTICATED` when token missing/invalid
- [ ] request rejected with `PERMISSION_DENIED` when request `userId` does not match token subject
- [ ] public auth methods remain callable without token

## C) Domain behavior correctness

- [ ] deterministic output for stable input fixture
- [ ] progression branch validated for strong readiness scenario
- [ ] steady branch validated for moderate scenario
- [ ] conservative branch validated for poor recovery/high strain scenario
- [ ] severe risk guard validated (mirror/reduction behavior)

## D) Rules and configuration

- [ ] rules JSON version exists and loads through `RulesConfigPort`
- [ ] unknown rules version returns controlled error path
- [ ] modality defaults/fallbacks are explicitly tested

## E) Data prerequisites

- [ ] last progression input is available for recommendation scenarios
- [ ] missing-history path returns `NOT_FOUND`
- [ ] user existence is validated before progression compute

## F) Integration test coverage

- [ ] end-to-end gRPC happy path passes (`AdaptiveTrainingFlowGrpcIntegrationTest`)
- [ ] recommendation includes at least one set prescription
- [ ] prescribed values (reps/load) match expected fixture tolerance

## G) Operational checks

- [ ] no runtime bean wiring regressions after module/config changes
- [ ] startup succeeds with required JWT secret configuration
- [ ] compose/local runbook updated when ports or env vars change

## H) Documentation updates

- [ ] `README.md` reflects current architecture
- [ ] `READMEAPI.md` reflects current RPC contract
- [ ] progression docs in this folder reflect current decision model
