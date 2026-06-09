# SignalScout

SignalScout is a personal market research radar for investment memos. It ingests research documents, extracts structured investment events, scores companies with deterministic rules, tracks portfolio context, and generates research reports.

This is a research and decision-support tool. It does not place trades.

## Current Phase

Phases 1-3 are implemented, and Phase 4 has started:

- Spring Boot API with manual document extraction endpoint
- OpenAI structured event extraction, with mock extraction when no API key is set
- Postgres/Flyway schema for companies, documents, events, scores, portfolio, reports, and jobs
- Deterministic Phase 1 scoring: Buy, Watch, Hold, Avoid
- Next.js watchlist and company detail screens
- Backend tests for scoring, hashing, JSON extraction mapping, health, and API error handling
- Phase 2 foundation: document chunking, stored-document keyword search, and job run history endpoints
- Phase 2 ingestion: SEC EDGAR ingestion, RSS/Atom ingestion, embedding job endpoints, and semantic-search API surface
- Phase 3 portfolio workflow: accounts, holdings, theses, risk settings, persisted recommendations, and on-demand portfolio action reports
- Phase 4 reporting workflow: daily/weekly/action report generation, saved report history, upcoming workflow items, browser PDF download, and scheduled PDF email delivery

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
- Portfolio workspace: `http://localhost:3000/portfolio`
- Reports workspace: `http://localhost:3000/reports`
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

Scheduled ingestion is off by default for local development:

```text
INGESTION_SCHEDULER_ENABLED=false
REPORT_SCHEDULER_ENABLED=false
INGESTION_RSS_FEEDS=https://example.com/feed.xml|NVDA,https://example.com/macro.xml
SEC_USER_AGENT=SignalScout your-email@example.com
```

For easier local/prod switching, use the ingestion settings templates:

```powershell
Copy-Item config\ingestion.local.env.example config\ingestion.local.env
```

For local email settings, use:

```powershell
Copy-Item config\email.local.env.example config\email.local.env
```

Then paste your Gmail app password into `config\email.local.env`:

```text
SMTP_PASSWORD=your-16-character-gmail-app-password
```

Scheduled report emails use this same file.

Or use the dedicated report scheduler file:

```powershell
Copy-Item config\reports.local.env.example config\reports.local.env
```

`config\reports.local.env` is loaded automatically by `scripts\backend-dev.ps1`.

Manual report PDFs can be generated from `http://localhost:3000/reports` by generating/selecting a report and clicking `Download PDF`. Scheduled report PDFs are saved to report history and emailed automatically when `REPORT_SCHEDULER_ENABLED=true`.

`scripts\backend-dev.ps1` automatically loads `.env.local`, `config\ingestion.local.env`, `config\email.local.env`, and `config\reports.local.env` if they exist. Keep real local/prod files untracked; only the `.example` templates belong in Git.

## Project Layout

```text
backend/   Spring Boot API
frontend/  Next.js dashboard
infra/     Docker Compose and Postgres init SQL
docs/      API, database, deployment, extraction docs
scripts/   Local tool install and build/dev helpers
```
