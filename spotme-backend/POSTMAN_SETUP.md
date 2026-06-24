# Testing SpotMe with Postman gRPC

This guide covers local testing of SpotMe's gRPC-native API using Postman.

## 1) Prerequisites

- Docker Desktop
- Postman (desktop app with gRPC support)
- Project root: `spotme-backend/`

## 2) Start the stack

```powershell
Copy-Item .env.example .env
docker compose up --build
```

Services:

- PostgreSQL on `localhost:5432`
- gRPC API on `localhost:9090`

## 3) Create a gRPC request in Postman

1. Click **New** -> **gRPC**
2. Server URL: `localhost:9090`
3. Import proto file:
   - `proto/src/main/proto/plan/v1/plan.proto`
4. Select service + method from schema browser

## 4) Auth flow in Postman

### Register

Method: `com.spotme.proto.plan.v1.AuthService/RegisterCredentials`

Example message:

```json
{
  "email": "alpha-tester@spotme.dev",
  "password": "Password123!",
  "experienceLevel": "beginner",
  "trainingGoal": "strength",
  "baselineSleepHours": 7,
  "stressSensitivity": 3
}
```

### Login

Method: `com.spotme.proto.plan.v1.AuthService/Login`

```json
{
  "email": "alpha-tester@spotme.dev",
  "password": "Password123!"
}
```

Save `accessToken` and `refreshToken` from response.

## 5) Set metadata for protected calls

In Postman gRPC request, open **Metadata** and add:

- Key: `authorization`
- Value: `Bearer <accessToken>`

Use this for all `PlanService` methods and authenticated `AuthService` methods.

## 6) Workout flow calls

- `PlanService/StartWorkoutSession`
- `PlanService/LogSet`
- `PlanService/CompleteWorkoutSession`
- `PlanService/Recommend`
- `PlanService/GetLatestWorkoutSession`
- `PlanService/ListRecentWorkoutSessions`

Use the same `userId` returned by registration/login-related flow.

## 7) Troubleshooting

### Cannot connect to `localhost:9090`

```powershell
docker compose ps
docker compose logs -f spotme-api
```

### Auth errors (`UNAUTHENTICATED`)

- Verify metadata key is exactly `authorization`
- Verify value prefix is exactly `Bearer `
- Re-login to get a fresh access token

### Permission errors (`PERMISSION_DENIED`)

- Ensure request `userId` matches JWT subject (`sub`)
- Do not call deprecated `PlanService/RegisterUser`

### Reflection not listing services

If reflection is disabled in your runtime profile, import proto definitions directly in Postman and select methods from imported schema.

## 8) Optional grpcurl sanity checks

```powershell
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext localhost:9090 describe com.spotme.proto.plan.v1.AuthService
```
