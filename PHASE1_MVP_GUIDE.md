# SignalScout Phase 1 MVP - Setup & Usage Guide

## Overview

This is the Phase 1 MVP implementation of SignalScout, a personal investment research assistant that:
- Accepts manual document paste (earnings transcripts, news, etc.)
- Uses OpenAI to extract investment events with structured outputs
- Computes deterministic stock scores
- Generates Buy/Watch/Hold/Avoid recommendations
- Tracks companies and events in a watchlist

## Project Structure

```
backend/
  src/main/java/com/scout/
    domain/
      entity/          # JPA entities (Company, RawDocument, ExtractedEvent, etc.)
      repository/      # Spring Data repositories
    application/
      dto/             # Request/response DTOs
      service/         # Business logic (DocumentService, ScoringService, OpenAIExtractionService)
    presentation/
      controller/      # REST API controllers
    infrastructure/
      exception/       # Global exception handler
      util/            # Utilities (HashUtil for content deduplication)
      config/          # Spring configuration (ObjectMapper bean)
    SignalScoutApplication.java

frontend/
  app/
    layout.tsx         # Root layout with providers
    page.tsx           # Home page
    watchlist/
      page.tsx         # Watchlist page
    company/
      [ticker]/
        page.tsx       # Company detail page
  components/
    ExtractionResult.tsx     # Modal showing extraction results
    EventCard.tsx            # Card component for extracted events
    ManualPasteForm.tsx      # Form to paste documents
    WatchlistGrid.tsx        # Grid of companies for watchlist
  lib/
    api-client.ts      # Axios client for backend API
    types.ts           # TypeScript interfaces
  globals.css          # Tailwind CSS styles
```

## Backend Setup

### Prerequisites
- Java 21+
- Gradle 8.0+
- PostgreSQL 14+ with pgvector extension
- OpenAI API key (for document extraction)

### Database Setup

1. **Create Postgres database**:
```sql
createdb signalscout
psql signalscout < infra/postgres-init/001_init_schema.sql
```

2. **Enable pgvector**:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### Environment Variables

Create a `.env` file in the backend directory:

```bash
# Database
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/signalscout
export SPRING_DATASOURCE_USERNAME=scout
export SPRING_DATASOURCE_PASSWORD=scout_dev_password

# OpenAI
export OPENAI_API_KEY=sk-xxx... # Your OpenAI API key
```

Load environment variables:
```bash
source .env
```

### Build & Run Backend

```bash
cd backend

# Build
gradle build

# Run
gradle bootRun

# Server will be available at http://localhost:8080/api
```

## Frontend Setup

### Prerequisites
- Node.js 18+
- npm or yarn

### Installation

```bash
cd frontend

# Install dependencies
npm install

# Set API URL (optional, defaults to http://localhost:8080/api)
export NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

### Run Frontend

```bash
# Development server
npm run dev

# Frontend will be available at http://localhost:3000
```

## Usage Workflow

### 1. Initialize Sample Data

Navigate to http://localhost:3000/watchlist and click "Initialize Sample Data". This will:
- Create 5 sample companies: MRVL, AVGO, NVDA, MSFT, AMD
- Create themes: AI/ML, Semiconductors, Enterprise Software
- Create data sources: SEC, Manual

### 2. Extract Document Events

**Via Frontend:**
1. Go to company detail page (e.g., http://localhost:3000/company/NVDA)
2. Scroll down to "Extract New Event" form
3. Paste any investment document (earnings transcript, news article, research note)
4. Click "Extract Investment Event"
5. View extraction results including:
   - Event type (earnings, guidance, partnership, etc.)
   - Bullish/bearish scores
   - Bull case, bear case, risks, watch items
   - Stock score and recommendation (Buy/Watch/Hold/Avoid)

**Via API:**

```bash
curl -X POST http://localhost:8080/api/documents/manual \
  -H "Content-Type: application/json" \
  -d '{
    "rawText": "NVIDIA earned $X per share...",
    "companyId": "1",
    "sourceType": "Manual"
  }'
```

### 3. View Company Details

Navigate to http://localhost:3000/company/[ticker] to see:
- Latest stock score (0-100)
- Latest recommendation (Buy/Watch/Hold/Avoid)
- History of extracted events
- Manual event paste form

### 4. Browse Watchlist

Go to http://localhost:3000/watchlist to see:
- All companies with latest scores
- Latest recommendations
- Recent event counts
- Click on any company to view details

## API Endpoints

### Documents
- `POST /api/documents/manual` - Paste document for extraction
  - Request: `{ rawText, companyId, sourceType }`
  - Response: Extracted event with stock score and recommendation

### Companies
- `GET /api/companies/{ticker}` - Get company detail
  - Response: Company info, latest score, recent events
- `GET /api/companies/watchlist/summary` - Get all companies for watchlist
  - Response: List of companies with latest scores

### Admin
- `POST /api/admin/seed-companies` - Initialize 5 sample companies
- `POST /api/admin/seed-themes` - Initialize themes
- `POST /api/admin/seed-sources` - Initialize data sources

## Scoring Formula (Phase 1)

```
stockScore = (50 + (bullish - bearish) * 0.5) * (sourceQuality / 100)
stockScore = clamp(0, stockScore, 100)

Recommendation:
- BUY:   score >= 80
- WATCH: score 60-79
- HOLD:  score 40-59
- AVOID: score < 40
```

Note: This is simplified for Phase 1. Full scoring in Phase 2 will incorporate valuation, quality, momentum, risk factors per the design specification.

## OpenAI Structured Outputs

The extraction service uses OpenAI's Structured Outputs API (JSON mode with schema validation) to ensure:
- Consistent JSON schema matching docs/EXTRACTION.md
- Strict field validation
- Deterministic outputs

The schema is defined in `OpenAIExtractionService.getExtractionJsonSchema()`.

### Mock Extraction (No API Key)

If `OPENAI_API_KEY` is not set, the system uses mock extraction data for testing. The mock will:
- Create realistic event structures
- Have confidence score of 30 (low) to indicate it's not real
- Include a note that OpenAI API is not configured

This allows testing the frontend without API costs during development.

## Extraction Output Schema

Each extracted event includes:
```json
{
  "schema_version": "1.0",
  "ticker": "NVDA",
  "company_name": "NVIDIA Corporation",
  "event_type": "guidance_raise",
  "event_date": "2026-06-04",
  "summary": "Management raised FY2026 guidance by 15%",
  "bullish_score": 85,
  "bearish_score": 25,
  "source_quality_score": 95,
  "confidence_score": 92,
  "bull_case": ["Strong demand signals", "Better margins"],
  "bear_case": ["Already priced in", "Macro risks"],
  "risks": ["Customer concentration", "Competition"],
  "watch_items": ["Next guidance", "Market share trends"],
  "requires_manual_review": false
}
```

## Testing

### Backend Tests

```bash
cd backend
gradle test
```

Key test coverage:
- Document extraction and storage
- Stock score computation
- Recommendation logic
- Error handling for duplicate documents

### Frontend Development

```bash
cd frontend
npm run dev
npm run type-check  # TypeScript validation
```

## Deployment (Docker)

### Docker Compose (Local Development)

```bash
cd infra/docker-compose
docker-compose up -d
```

This starts:
- PostgreSQL 14 with pgvector
- Redis (for future caching)

### Production Deployment

See DEPLOYMENT.md for production setup instructions.

## Known Limitations (Phase 1)

1. No user authentication (all data is shared)
2. No SEC EDGAR polling yet (manual paste only)
3. No email integration yet
4. No portfolio tracking or position sizing
5. No backtesting or performance analytics
6. Simplified scoring (no valuation, momentum, quality factors)
7. No mobile app
8. No social media integration

These will be added in Phase 2+.

## Cost Tracking

The system logs OpenAI API usage:
```
OpenAI API usage: prompt=250 tokens, completion=150 tokens (est. cost ~$0.0015)
```

Each extraction typically costs ~$0.001-$0.003 depending on document length.

## Troubleshooting

### Database Connection Failed
- Ensure PostgreSQL is running
- Check SPRING_DATASOURCE_URL, username, password
- Verify pgvector extension is enabled: `psql signalscout -c "SELECT * FROM pg_extension WHERE extname='vector'"`

### OpenAI API Errors
- Verify OPENAI_API_KEY is set and valid
- Check API rate limits
- System will fall back to mock extraction if key is invalid

### Frontend Can't Connect to Backend
- Ensure backend is running on port 8080
- Check NEXT_PUBLIC_API_URL is set correctly
- Verify CORS is enabled (should be by default in Spring Boot)

### Build Failures
- Java 21+ required
- PostgreSQL dev headers required for pgvector: `sudo apt-get install postgresql-client libpq-dev`
- Clear Gradle cache: `gradle clean`

## Next Steps (Phase 2)

1. Add user authentication and multi-user support
2. Implement SEC EDGAR polling
3. Add Gmail/email document ingestion
4. Implement full scoring formula with valuation/quality/momentum
5. Add position tracking and portfolio constraints
6. Create buy/sell/hold action recommendations
7. Add risk assessment engine
8. Build investment thesis tracking

## Documentation

- [EXTRACTION.md](docs/EXTRACTION.md) - Extraction schema reference
- [API.md](docs/API.md) - API specification
- [DATABASE.md](docs/DATABASE.md) - Database schema
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) - Production deployment guide

## Support

For issues or questions, refer to the design specification document for Phase 1-6 architecture details.
