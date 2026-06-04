# SignalScout

SignalScout is a personal market research radar for investment memos. Phase 1 focuses on the manual workflow: paste a document, extract structured investment events, score the signal with deterministic rules, and view the result by company.

This is a research and decision-support tool. It does not place trades.

## Current Phase

Phase 1 MVP is implemented:

- Spring Boot API with manual document extraction endpoint
- OpenAI structured event extraction, with mock extraction when no API key is set
- Postgres/Flyway schema for companies, documents, events, scores, portfolio, reports, and jobs
- Deterministic Phase 1 scoring: Buy, Watch, Hold, Avoid
- Next.js watchlist and company detail screens
- Backend tests for scoring, hashing, JSON extraction mapping, health, and API error handling

## Local Tooling

This repo can use project-local tools under `.tools/`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\install-dev-tools.ps1
```

That installs portable Java 21, Node.js, and Gradle into `.tools/`. The directory is ignored by Git.

## Build And Test

Backend:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backend-build.ps1
```

Frontend:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\frontend-build.ps1
```

Verified on this workspace:

- `backend\gradlew.bat -p backend clean build`
- `npm run type-check`
- `npm run build`

## Run Locally

Start Postgres and Redis:

```powershell
docker compose -f infra\docker-compose\docker-compose.yml up -d
```

Then start the API:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\backend-dev.ps1
```

Start the frontend in a second terminal:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\frontend-dev.ps1
```

URLs:

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080/api`
- Health: `http://localhost:8080/api/health`

## Environment

The backend defaults to:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/signalscout
SPRING_DATASOURCE_USERNAME=scout
SPRING_DATASOURCE_PASSWORD=scout_dev_password
OPENAI_API_KEY=
```

If `OPENAI_API_KEY` is empty, the extraction service returns a mock extraction so the UI flow can still be tested.

The frontend defaults to:

```text
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Important Limitation

Docker is not installed in this execution environment, so the full database-backed app has not been runtime-tested end to end here. Backend and frontend builds pass. To run the app fully, install Docker Desktop or provide another local Postgres 16 instance with pgvector enabled.

## Project Layout

```text
backend/   Spring Boot API
frontend/  Next.js dashboard
infra/     Docker Compose and Postgres init SQL
docs/      API, database, deployment, extraction docs
scripts/   Local tool install and build/dev helpers
```
