# SignalScout

**SignalScout** is a personal investment research radar and decision-support tool. It ingests official company documents, emails, news, and transcripts; extracts structured investment events using AI; and generates portfolio action recommendations with deterministic scoring rules.

**Core Principle:** This is a research tool and watchlist manager, not an auto-trading bot.

## Tech Stack

- **Backend**: Java 21 + Spring Boot 3.x + Postgres + pgvector
- **Frontend**: Next.js + React + Tailwind CSS
- **AI**: OpenAI structured extraction + embeddings
- **Deployment**: Docker Compose (local), managed Postgres (cloud)

## Quick Start (Local Development)

### Prerequisites

- Docker & Docker Compose
- Java 21 (for backend development)
- Node.js 18+ (for frontend development)
- Git

### 1. Clone & Setup

```bash
cd c:\Users\tyb_l\SignalScout
git init
```

### 2. Start Docker Compose Stack

```bash
docker-compose -f infra/docker-compose/docker-compose.yml up -d
```

This starts:
- PostgreSQL 16 with pgvector extension
- Redis (optional, for job queues later)

Verify with:
```bash
docker ps
```

### 3. Run Database Migrations

```bash
# Copy SQL schema into Postgres
docker exec signalscout-postgres psql -U scout -d signalscout -f /docker-entrypoint-initdb.d/001_init_schema.sql
```

### 4. Build & Run Backend

```bash
cd backend
./gradlew build
./gradlew bootRun
```

Backend API runs on `http://localhost:8080`

### 5. Build & Run Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on `http://localhost:3000`

## Project Structure

```
signalscout/
├── README.md
├── docker-compose.yml
├── .env.example
│
├── backend/                        # Spring Boot API
│   ├── build.gradle
│   ├── src/main/java/com/scout/
│   │   ├── api/                   # REST controllers
│   │   ├── auth/                  # JWT/Spring Security
│   │   ├── company/               # Company management
│   │   ├── watchlist/             # Watchlist logic
│   │   ├── document/              # Document ingestion
│   │   ├── extraction/            # AI extraction service
│   │   ├── scoring/               # Scoring & recommendation engine
│   │   ├── portfolio/             # Portfolio holdings & accounts
│   │   ├── thesis/                # Investment thesis tracking
│   │   ├── job/                   # Scheduled jobs
│   │   └── common/                # Shared utils, exceptions
│   ├── src/test/
│   └── docker/Dockerfile
│
├── frontend/                       # Next.js dashboard
│   ├── app/
│   │   ├── dashboard/             # Main dashboard
│   │   ├── watchlist/             # Watchlist page
│   │   ├── company/[ticker]/      # Company detail page
│   │   ├── portfolio/             # Portfolio view
│   │   ├── reports/               # Reports & digests
│   │   └── settings/              # User settings
│   ├── components/
│   ├── lib/
│   ├── package.json
│   └── Dockerfile
│
├── infra/
│   ├── docker-compose/
│   │   └── docker-compose.yml
│   └── postgres-init/
│       └── 001_init_schema.sql    # Schema + pgvector setup
│
├── docs/
│   ├── API.md                     # API contracts
│   ├── DESIGN.md                  # Full design document reference
│   ├── EXTRACTION.md              # AI extraction prompts & schemas
│   ├── DATABASE.md                # Schema docs
│   └── DEPLOYMENT.md              # Production deployment
│
└── .github/
    └── workflows/
        └── ci-cd.yml              # GitHub Actions
```

## Development Phases

### Phase 1: Manual MVP (Current)
- Manual document paste endpoint
- AI extraction into structured JSON
- Company watchlist page
- Basic score display

### Phase 2: Core Research Radar
- SEC EDGAR ingestion
- RSS feed polling
- Document deduplication
- Embeddings & semantic search

### Phase 3: Portfolio-Aware Decisions
- Portfolio accounts/holdings
- Investment thesis tracking
- Scoring engine
- Buy/Hold/Sell recommendation rules

### Phase 4: Reports & Workflow
- Daily digest generation
- Weekly action report
- Markdown export

### Phase 5: Better Ingestion
- Gmail label ingestion
- Earnings transcript sources
- Source trust scoring

### Phase 6: Evaluation
- Recommendation backtest
- Outcome tracking
- Scoring tune-ups

## Environment Variables

Create `.env` in the root:

```bash
# Backend
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/signalscout
SPRING_DATASOURCE_USERNAME=scout
SPRING_DATASOURCE_PASSWORD=scout_dev_password

# AI
OPENAI_API_KEY=your-key-here
OPENAI_EXTRACTION_MODEL=gpt-4
OPENAI_EMBEDDING_MODEL=text-embedding-3-small

# App
APP_BASE_URL=http://localhost:3000
JWT_SECRET=dev-secret-change-in-prod

# Limits
AI_DAILY_COST_LIMIT_USD=10.00
```

## Building & Testing

**Backend**:
```bash
cd backend
./gradlew test       # Run tests
./gradlew build      # Build JAR
```

**Frontend**:
```bash
cd frontend
npm test             # Run tests
npm run build        # Build for production
```

## Database Migrations

Migrations are in `backend/src/main/resources/db/migration/`.

To add a new migration:
1. Create `V<version>__<description>.sql` in the migration folder
2. Flyway will run it automatically on app startup

## API Documentation

See [docs/API.md](docs/API.md) for REST endpoints.

## Key Design Principles

1. **AI is the analyst, rules are the decision-maker**: LLMs extract facts and draft memos. Deterministic scoring rules decide Buy/Hold/Sell.
2. **Every buy must beat a broad ETF**: Recommendations compare against XEQT, VFV, XQQ, etc.
3. **Social media is a smoke detector**: Low-trust sources only trigger watchlist items, never buys.
4. **Thesis required**: Single-stock buys must have a documented thesis with triggers and invalidation conditions.
5. **No auto-trading**: This tool supports research decisions, not algorithmic trading.

## Testing & Golden Cases

The app validates against golden test cases:

- **Positive earnings + guidance raise** → WATCH/BUY candidate (if score high enough)
- **Social hype only** → AVOID or low-priority WATCH
- **Guidance cut** → SELL/TRIM warning
- **Position too large** → TRIM (even if fundamentals good)
- **Single stock vs broad ETF** → ETF recommended if stock doesn't justify premium

## Contributing

This is a personal research project, but improvements welcome. Run the full test suite before submitting PRs.

## Security Notes

- Never commit secrets or API keys
- OAuth tokens encrypted at rest (Spring Security)
- Passwords hashed (bcrypt)
- Email content not logged
- User data export available on request

## License

Personal use only. Not licensed for redistribution or commercial use without legal review.

---

**Project Start**: 2026-06-04  
**Version**: 1.0 (Phase 1 MVP)  
**Reference**: Design document in `docs/DESIGN.md`
Scout signals. Skip hype. An AI-powered investment research radar that scans filings, earnings, news, emails, and transcripts to detect what changed, what matters, and what to watch next.
