# Document Management Platform

A microservice-based document management platform built with Kotlin Spring Boot and React (Vite).

## Architecture

```
React SPA (5173)
      |
API Gateway (8080)
  |      |      |      |
  |      |      |      +-- AI Processing Service (8084)
  |      |      +--------- File Service (8083)
  |      +---------------- Document Service (8082)
  +----------------------- User Service (8081)

Infra:
- PostgreSQL (5432) [user_db, document_db, file_db, ai_db, keycloak]
- MinIO (9000/9001)
- Keycloak (8085)
- Kafka (9092, KRaft mode)
- Ollama (11434) for AI summaries/tags
```

## Prerequisites

- JDK 17+
- Node.js 20+
- Docker + Docker Compose
- Gradle 8+ (or use `./gradlew` / `gradlew.bat`)
- Ollama installed locally



## Quick Setup (Assumes Docker & Ollama Already Installed)

### Step 1: Start Docker Infrastructure

```bash
cd docker
docker-compose -f docker-compose.infra.yml up -d
docker ps  # Verify containers are running
```

**Expected containers:** PostgreSQL, MinIO, Keycloak, Kafka, Grafana, Prometheus, Jaeger

---

### Step 2: Pull Ollama Model

```bash
ollama pull llama3.2:3b
ollama list  # Verify model is loaded
```

Then in another terminal, keep Ollama running:
```bash
ollama serve
```

---

### Step 3: Configure Keycloak (IMPORTANT!)

**Access Keycloak Admin:**
- Open: `http://localhost:8085`
- Login: `admin` / `Admin123!`

**3.1: Create Realm `docplatform`**
- Click **"Select Realm"** → **"Create Realm"**
- Name: `docplatform`
- Click **"Create"**

**3.2: Create Roles (All 4 are required)**
- Left sidebar → **"Realm roles"**
- Click **"Create role"** for each:
  - `ADMIN`
  - `MANAGER`
  - `USER`
  - `STAFF` ← **Important for AI Q&A**

**3.3: Create Client `docplatform-frontend`**
- Left sidebar → **"Clients"**
- Click **"Create client"**
- **Client ID:** `docplatform-frontend`
- **Type:** OpenID Connect
- Click **"Next"** → **"Next"** again

**3.4: Configure Client Redirect URIs**
- **Valid redirect URIs:** `http://localhost:5173/*`
- **Valid post logout redirect URIs:** `http://localhost:5173/*`
- **Web origins:** `http://localhost:5173`
- Click **"Save"**

**3.5: Configure Token Mappers**
- Go to **"Client scopes"** tab
- Click **`docplatform-frontend-dedicated`** (or the assigned scope)
- Go to **"Mappers"** tab
- Click **"realm roles"** mapper
- Verify and save:
  - ✅ **Add to ID Token:** On
  - ✅ **Add to access Token:** On
  - ✅ **Multivalued:** On

**3.6: Create Test User**
- Left sidebar → **"Users"** → **"Create user"**
- Username: `testuser`
- Click **"Create"**
- **Credentials tab:** Set password `Test123!` (uncheck "Temporary")
- **Role mapping tab:** Click **"Assign role"** → Select `STAFF` → **"Assign"**
- **Logout** and verify login works

---

### Step 4: Build Backend Services

```bash
# From project root
gradlew.bat clean build -x test
```

---

### Step 5: Run Backend Services (5 Separate Terminals)

**Terminal 1 - API Gateway:**
```bash
gradlew.bat :services:api-gateway:bootRun
```

**Terminal 2 - User Service:**
```bash
gradlew.bat :services:user-service:bootRun
```

**Terminal 3 - Document Service:**
```bash
gradlew.bat :services:document-service:bootRun
```

**Terminal 4 - File Service:**
```bash
gradlew.bat :services:file-service:bootRun
```

**Terminal 5 - AI Processing Service:**
```bash
gradlew.bat :services:ai-processing-service:bootRun
```

Each should print `Started [ServiceName]Application` when ready.

---

### Step 6: Run Frontend

```bash
cd frontend
npm install
copy .env.example .env
npm run dev
```

**Access:** `http://localhost:5173`

**Login with:**
- Username: `testuser`
- Password: `Test123!`

---

### Step 7: Test It Works

1. ✅ Login at `http://localhost:5173`
2. ✅ Upload a document
3. ✅ Ask AI a question (should respond)

---

## Project Structure

```
final-project-g10/
├── build.gradle.kts
├── settings.gradle.kts
├── shared/
│   ├── common-api/
│   └── common-security/
├── services/
│   ├── api-gateway/
│   ├── user-service/
│   ├── document-service/
│   ├── file-service/
│   └── ai-processing-service/
├── frontend/
└── docker/
    ├── docker-compose.infra.yml
    ├── docker-compose.dev.yml
    └── docker-compose.yml
```

## Services

### API Gateway (`8080`)

- Routes all `/api/v1/**` traffic to downstream services
- Validates JWT tokens
- Handles CORS and fallbacks

### User Service (`8081`)

- User/departments/roles operations

### Document Service (`8082`)

- Document metadata, ACL/sharing, versions

### File Service (`8083`)

- TUS upload flow
- MinIO storage + downloads + presigned URLs
- Publishes `FileUploadedEvent` to Kafka

### AI Processing Service (`8084`)

- Consumes `file-uploaded` Kafka events
- Runs background processing
- Generates summary/tags and processing metadata
- Exposes processing status/result APIs

## Automated Document Processing (Requirement 5)

### a) Background processing after upload

- Upload completion in file-service publishes `FileUploadedEvent`
- AI processing service consumes event from Kafka
- Processing is done asynchronously and persisted in `ai_db`

### b) Non-blocking user operations

- Upload and document creation return immediately
- AI work runs in background (`@Async`) and updates later

### c) Exposed status/outcome

Endpoints:

```text
GET  /api/v1/processing/status/file/{fileId}
GET  /api/v1/processing/result/file/{fileId}
POST /api/v1/processing/retry/{processingId}
```

Frontend displays badges such as `AI pending`, `AI processing`, `AI ready`, `AI failed`.

## API Overview

### User / Departments

```text
GET/PUT  /api/v1/users/me
GET      /api/v1/users
GET/POST /api/v1/departments
POST/DEL /api/v1/departments/{id}/members
```

### Documents

```text
GET/POST    /api/v1/documents
GET/PUT/DEL /api/v1/documents/{id}
POST        /api/v1/documents/{id}/share
GET/DEL     /api/v1/documents/{id}/shares
```

### Files

```text
POST   /api/v1/upload
PATCH  /api/v1/upload/{id}
HEAD   /api/v1/upload/{id}
GET    /api/v1/files/{id}/download
GET    /api/v1/files/{id}/presigned
```

### AI Processing

```text
GET    /api/v1/processing/status/file/{fileId}
GET    /api/v1/processing/result/file/{fileId}
POST   /api/v1/processing/retry/{processingId}
```

## Environment Variables

### Frontend (`frontend/.env`)

- `VITE_API_BASE_URL` (default `/api/v1`)
- `VITE_KEYCLOAK_AUTHORITY` (e.g. `http://localhost:8085/realms/docplatform`)
- `VITE_KEYCLOAK_CLIENT_ID` (e.g. `docplatform-frontend`)
- `VITE_REDIRECT_URI` (e.g. `http://localhost:5173/callback`)
- `VITE_POST_LOGOUT_REDIRECT_URI` (e.g. `http://localhost:5173`)

### Backend (important)

- `KEYCLOAK_ISSUER_URI` (default local realm)
- `KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `MINIO_ENDPOINT` (default `http://localhost:9000`)

### AI service (Ollama)

- `AI_OLLAMA_ENABLED` (default `true`)
- `AI_OLLAMA_BASE_URL` (default `http://localhost:11434`)
- `AI_OLLAMA_MODEL` (default `llama3.2:3b`)

## Troubleshooting

### Ollama port already in use (`11434`)

If `ollama serve` says port is already in use, Ollama is likely already running. Verify:

```bash
ollama list
curl http://localhost:11434/api/tags
```

### Kafka connection errors (`localhost:9092`)

Restart Kafka from infra compose:

```bash
cd docker
docker-compose -f docker-compose.infra.yml up -d kafka
```

### Keycloak redirect URI errors

Recheck client config in Keycloak:

- Valid redirect URIs: `http://localhost:5173/*`
- Post logout redirect URIs: `http://localhost:5173/*`
- Web origins: `http://localhost:5173`

### AI summary still shows old/placeholder output

Processing result is stored per file/version. Upload a new version (or new file) to trigger fresh processing with the current pipeline.

### AI Q&A "Sorry, I encountered an error" (Timeout)

**Problem:** Asking AI a question returns: "I/O error on POST request for `http://localhost:11434/api/chat`: timeout"

**Checklist:**
1. ✅ Ollama running? → `ollama serve` in a terminal
2. ✅ Model downloaded? → `ollama list` should show `llama3.2:3b`
3. ✅ User has STAFF role? → Check Keycloak (Step 3.6 above)
4. ✅ First request slow? → Wait 30-120 seconds (model loads into memory)

**Quick fix:**
```bash
# Verify Ollama is accessible
curl http://localhost:11434/api/tags

# Pull model if missing
ollama pull llama3.2:3b

# Restart AI service (has 120-second timeout configured)
taskkill /F /IM java.exe
gradlew.bat :services:ai-processing-service:bootRun
```

### Port already in use (Java services)

Windows quick cleanup:

```powershell
taskkill /F /IM java.exe
```


## Observability & Monitoring (Optional)

Once Docker infrastructure is running, monitoring dashboards are available:

* **Grafana:** `http://localhost:3000` (admin / admin)
* **Prometheus:** `http://localhost:9090`
* **Jaeger:** `http://localhost:16686`
