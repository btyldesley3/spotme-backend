# SpotMe Local Development with Docker

## Quick Start

### Prerequisites
- Docker & Docker Compose installed
- Git cloned at `spotme-backend/`

### 1. First-time setup

```bash
cd spotme-backend

# Copy environment file
cp .env.example .env

# Build and start containers
docker compose up --build
```

This will:
- Build the `spotme-api` image from `Dockerfile`
- Start PostgreSQL on `localhost:5432`
- Start the app on REST `localhost:8080` + gRPC `localhost:9090`
- Wait for DB health check before app starts

**First build takes ~3-5 minutes** (Maven compiles everything). Subsequent builds are faster.

---

## Testing the API

### Option 1: cURL (Terminal)

**Start a workout session:**
```bash
curl -X POST "http://localhost:8080/api/v1/workout-sessions/start" \
  -H "Content-Type: application/json" \
  -d '{"userId":"11111111-1111-1111-1111-111111111111","startedAt":"2026-04-14T18:00:00Z"}'
```

Copy the returned `sessionId` for the next step.

**Log a set:**
```bash
curl -X POST "http://localhost:8080/api/v1/workout-sessions/{sessionId}/sets" \
  -H "Content-Type: application/json" \
  -d '{"userId":"11111111-1111-1111-1111-111111111111","exerciseId":"22222222-2222-2222-2222-222222222222","setNumber":1,"reps":8,"weightKg":60.0,"rpe":8.0,"note":"top set"}'
```

**Complete session + report recovery:**
```bash
curl -X POST "http://localhost:8080/api/v1/workout-sessions/{sessionId}/complete" \
  -H "Content-Type: application/json" \
  -d '{"userId":"11111111-1111-1111-1111-111111111111","finishedAt":"2026-04-14T18:45:00Z","minTotalSets":1,"minDistinctExercises":1,"minSetsPerExercise":1,"requireRecoveryFeedbackForProgression":true,"doms":3,"sleepQuality":7}'
```

**Get recommendation:**
```bash
curl -X POST "http://localhost:8080/api/v1/recommendations" \
  -H "Content-Type: application/json" \
  -d '{"userId":"11111111-1111-1111-1111-111111111111","exerciseId":"22222222-2222-2222-2222-222222222222","rulesVersion":"v1.0.0","modalityKey":"barbell_upper"}'
```

### Option 2: Postman

1. Open Postman
2. Create new requests pointing to `http://localhost:8080/api/v1/...`
3. Use the cURL examples above as request bodies

---

## Debugging

### View container logs
```bash
# All containers
docker compose logs -f

# Just the API
docker compose logs -f spotme-api

# Just Postgres
docker compose logs -f postgres
```

### Connect to Postgres directly
```bash
docker exec -it spotme-postgres psql -U spotme -d spotme
```

### Stop everything
```bash
docker compose down
```

### Stop + remove data (clean slate)
```bash
docker compose down -v
```

---

## Switching to Amazon RDS (Future)

When you're ready to use Amazon RDS instead of local Postgres:

1. Set environment variables in your deploy platform (e.g., ECS, Lambda, etc.):
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://<your-rds-endpoint>:5432/spotme
   SPRING_DATASOURCE_USERNAME=<rds_username>
   SPRING_DATASOURCE_PASSWORD=<rds_password>
   ```

2. Build the image with Docker (same Dockerfile):
   ```bash
   docker build -t spotme-api:latest .
   docker push <your-ecr-repo>/spotme-api:latest
   ```

3. Deploy to AWS (ECS, EKS, etc.) — no code changes needed!

---

## Common Issues

**Issue:** API won't start / connection refused

- **Check Postgres is healthy:** `docker compose logs postgres`
- **Check API logs:** `docker compose logs spotme-api`
- **Rebuild:** `docker compose up --build --force-recreate`

**Issue:** Port 8080/9090 already in use

- Edit `docker-compose.yaml` and change `ports` to different host ports, e.g., `"8081:8080"`

**Issue:** Build takes forever

- First build is slow (Maven downloading deps). **Subsequent builds are cached.**
- If you want to skip tests during build, edit `Dockerfile` and change `DskipTests` to `true`.

---

## Next Steps

- [ ] Add database migrations via Flyway (currently disabled for MVP)
- [ ] Add Redis caching (commented out in compose)
- [ ] Production Dockerfile optimization (multi-stage is already optimized)

