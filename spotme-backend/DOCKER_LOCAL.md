# SpotMe Local Development with Docker

This guide runs SpotMe locally with Docker Compose using the gRPC-native API.

## 1) Prerequisites

- Docker Desktop
- Repo cloned at `spotme-backend/`

## 2) First-time setup

```powershell
Copy-Item .env.example .env
docker compose up --build
```

What starts:

- `postgres` on `localhost:5432`
- `spotme-api` gRPC server on `localhost:9090`

## 3) Daily commands

Start:

```powershell
docker compose up
```

Stop:

```powershell
docker compose down
```

Clean slate (remove volumes):

```powershell
docker compose down -v
```

Logs:

```powershell
docker compose logs -f
docker compose logs -f spotme-api
docker compose logs -f postgres
```

## 4) Verify gRPC service

```powershell
grpcurl -plaintext localhost:9090 list
```

Expected service list includes:

- `com.spotme.proto.plan.v1.AuthService`
- `com.spotme.proto.plan.v1.PlanService`

## 5) Common issues

### `spotme-api` fails to boot

- Check DB health and startup order in logs
- Verify `.env` values are valid
- Rebuild from scratch:

```powershell
docker compose up --build --force-recreate
```

### Port conflict on `5432` or `9090`

Edit `docker-compose.yaml` host-side mapping to free ports, then restart compose.

### JWT startup failure

Outside `test` profile, `spotme.jwt.secret` must be set and decode to at least 32 bytes.

## 6) Postgres shell access

```powershell
docker exec -it spotme-postgres psql -U spotme -d spotme
```

## 7) Notes

- Flyway behavior is controlled by `SPRING_FLYWAY_ENABLED`
- Redis-related modules exist but are not required for MVP gRPC flow
- For interactive API calls, use Postman gRPC or `grpcurl`
