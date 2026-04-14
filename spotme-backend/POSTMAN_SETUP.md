# Testing SpotMe API with Postman via Docker

This guide walks you through testing the SpotMe REST API endpoints using Postman with your Docker setup.

---

## 📋 Prerequisites

1. **Docker & Docker Compose** installed and running
2. **Postman** installed ([download here](https://www.postman.com/downloads/))
3. **SpotMe containers running** (see Quick Start below)

---

## 🚀 Quick Start

### Step 1: Start Docker Containers

From your project root (`spotme-backend/`):

```bash
# First time: copy environment file
cp .env.example .env

# Build and start containers
docker compose up --build
```

**What this does:**
- Builds the `spotme-api` Docker image (3-5 min on first run)
- Starts PostgreSQL on `localhost:5432`
- Starts the REST API on `http://localhost:8080`
- Starts the gRPC server on `localhost:9090`

**Check if containers are running:**
```bash
docker compose logs -f spotme-api
```

Look for: `Tomcat started on port(s): 8080` and `gRPC Server started` message.

---

### Step 2: Import Postman Collection

1. **Open Postman**
2. Click **Import** (top-left)
3. Select **File** tab → **Upload Files**
4. Navigate to `SpotMe-API.postman_collection.json` in your project root
5. Click **Import**

You should now see the "SpotMe API - Local Docker" collection in your left sidebar with all endpoints organized.

---

## 📡 Testing Workflow

### Test Flow (In Order):

#### **1️⃣ Start a Workout Session**
- **Request:** `POST http://localhost:8080/api/v1/workout-sessions/start`
- **Body:**
  ```json
  {
    "userId": "11111111-1111-1111-1111-111111111111",
    "startedAt": "2026-04-14T18:00:00Z"
  }
  ```
- **Response:** You'll get a `sessionId` (looks like a UUID)
  ```json
  {
    "sessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "userId": "11111111-1111-1111-1111-111111111111",
    "startedAt": "2026-04-14T18:00:00Z"
  }
  ```

**💡 Pro Tip:** Copy the `sessionId` from the response and paste it into Postman's **Variables** section (at the bottom of the collection) under the `sessionId` variable. This lets you use `{{sessionId}}` in other requests.

---

#### **2️⃣ Log Sets (Optional)**
- **Request:** `POST http://localhost:8080/api/v1/workout-sessions/{{sessionId}}/sets`
- **Body:**
  ```json
  {
    "userId": "11111111-1111-1111-1111-111111111111",
    "exerciseId": "22222222-2222-2222-2222-222222222222",
    "setNumber": 1,
    "reps": 8,
    "weightKg": 60.0,
    "rpe": 8.0,
    "note": "top set"
  }
  ```
- **Response:**
  ```json
  {
    "sessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "totalSetsInSession": 1
  }
  ```

**Repeat this 2-3 times** with different `setNumber` values (2, 3, etc.) to log multiple sets.

---

#### **3️⃣ Get Recommendation (Optional)**
- **Request:** `POST http://localhost:8080/api/v1/recommendations`
- **Body:**
  ```json
  {
    "userId": "11111111-1111-1111-1111-111111111111",
    "exerciseId": "22222222-2222-2222-2222-222222222222",
    "rulesVersion": "v1.0.0",
    "modalityKey": "barbell_upper"
  }
  ```
- **Response:** Progression recommendation (weight, reps for next workout)
  ```json
  {
    "sets": [
      {
        "exerciseId": "22222222-2222-2222-2222-222222222222",
        "order": 1,
        "prescribedReps": 8,
        "prescribedWeightKg": 60.25,
        "isBackoff": false
      }
    ]
  }
  ```

---

#### **4️⃣ Complete Workout Session**
- **Request:** `POST http://localhost:8080/api/v1/workout-sessions/{{sessionId}}/complete`
- **Body:**
  ```json
  {
    "userId": "11111111-1111-1111-1111-111111111111",
    "finishedAt": "2026-04-14T18:45:00Z",
    "minTotalSets": 1,
    "minDistinctExercises": 1,
    "minSetsPerExercise": 1,
    "requireRecoveryFeedbackForProgression": true,
    "doms": 3,
    "sleepQuality": 7
  }
  ```
- **Response:** Session completion summary
  ```json
  {
    "sessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "completed": true,
    "allowsProgression": true,
    "completionReason": "Session valid for progression",
    "totalExercises": 1,
    "totalSets": 1,
    "totalReps": 8,
    "totalVolumeKg": 480.0
  }
  ```

---

#### **5️⃣ Query Recent Sessions**
- **Request:** `GET http://localhost:8080/api/v1/workout-sessions/recent?userId=11111111-1111-1111-1111-111111111111&limit=10`
- **Response:** List of recent sessions for the user

---

### Advanced: Using Postman Variables

Store frequently-used values in Postman to avoid copy-pasting:

1. Click the **eye icon** (top-right) → **Environments**
2. Create a new environment named "Local Docker"
3. Add variables:
   - `base_url`: `http://localhost:8080/api/v1`
   - `userId`: `11111111-1111-1111-1111-111111111111`
   - `exerciseId`: `22222222-2222-2222-2222-222222222222`
   - `sessionId`: (empty initially; fill after starting session)

4. Update request URLs to use `{{base_url}}/workout-sessions/start`

---

## 🐛 Troubleshooting

### Issue: "Connection refused" on localhost:8080

**Solution:**
```bash
# Check if containers are running
docker compose ps

# If not running, rebuild:
docker compose up --build

# View API logs to find the error
docker compose logs spotme-api
```

### Issue: Port 8080/9090 already in use

**Solution:** Edit `docker-compose.yaml` and change port mappings:
```yaml
ports:
  - "8081:8080"  # Changed from 8080:8080
  - "9091:9090"  # Changed from 9090:9090
```

Then update Postman URLs to `http://localhost:8081/api/v1/...`

### Issue: "Cannot parse request body" errors

**Solution:**
- Make sure **Content-Type** header is set to `application/json`
- Validate JSON syntax (use Postman's JSON validation or an online tool)
- Check that all **required** fields are present

### Issue: gRPC port is working but REST isn't

**Solution:** Check if the REST adapter is enabled in Spring:
```bash
# Look for this in logs:
docker compose logs spotme-api | grep -i "RestController\|RequestMapping"

# If not found, the REST module might be disabled
# Check app/pom.xml to ensure in.rest dependency is included
```

---

## 📚 API Endpoint Reference

| Method | Endpoint | Purpose |
|--------|----------|---------|
| **POST** | `/workout-sessions/start` | Start a new workout |
| **POST** | `/workout-sessions/{sessionId}/sets` | Log an exercise set |
| **POST** | `/workout-sessions/{sessionId}/complete` | Finish workout + feedback |
| **GET** | `/workout-sessions/latest?userId=...` | Get user's latest session |
| **GET** | `/workout-sessions/recent?userId=...&limit=10` | Get recent sessions |
| **POST** | `/recommendations` | Get progression advice |

---

## 🔧 Database Access (Optional)

Connect to PostgreSQL container directly to inspect data:

```bash
# Access Postgres shell
docker exec -it spotme-postgres psql -U spotme -d spotme

# Useful queries:
# List tables: \dt
# Query sessions: SELECT * FROM workout_sessions;
# Quit: \q
```

---

## 📦 Next Steps

- **Test with cURL:** Use examples in `DOCKER_LOCAL.md`
- **Test gRPC:** Use a gRPC client (grpcurl, Postman gRPC support, or custom client)
- **Load Testing:** Use Postman's Collection Runner to simulate multiple requests

---

## 💬 Tips

- **Use Postman's Tests tab** to automate assertions (e.g., `pm.response.code === 200`)
- **Use pre-request scripts** to generate dynamic data (timestamps, UUIDs)
- **Export results** from Collection Runner for documentation
- **Share collections** with teammates via Postman Teams

Happy testing! 🎉

