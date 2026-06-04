# SignalScout Database Design

## Schema Overview

The database is designed around key entities:

1. **Users** - Application users
2. **Companies** - Publicly traded companies with basic info
3. **Watchlist** - User's watched companies
4. **Themes** - Investment themes (AI, Semi, etc.)
5. **Data Sources** - Ingestion sources (SEC, Gmail, RSS, Manual)
6. **Documents** - Raw documents from sources
7. **Events** - Extracted investment events from documents
8. **Scores** - Computed investment scores and recommendations
9. **Portfolio** - User holdings and accounts
10. **Theses** - Investment theses and tracking

## Key Tables

### companies
```sql
ticker TEXT UNIQUE                  -- "MRVL", "AVGO"
name TEXT                          -- "Marvell Technology"
cik TEXT                           -- SEC CIK for EDGAR integration
sector TEXT                        -- "Semiconductors"
currency TEXT DEFAULT 'CAD'        -- Base currency
```

### raw_documents
```sql
company_id BIGINT                  -- Which company
source_id BIGINT                   -- Data source
source_type TEXT                   -- "SEC_FILING", "EARNINGS_CALL", "MANUAL_PASTE"
content_hash TEXT UNIQUE           -- For deduplication
raw_text TEXT                      -- Full document text
processing_status TEXT             -- "new", "processing", "completed", "failed"
```

### extracted_events
```sql
company_id BIGINT                  -- Which company
event_type TEXT                    -- "earnings_release", "guidance_raise", "partnership"
event_date DATE                    -- When the event occurred
summary TEXT                       -- AI-generated summary
bull_case JSONB                    -- Array of bullish points
bear_case JSONB                    -- Array of bearish points
risks JSONB                        -- Array of risk factors
bullish_score INT (0-100)         -- How bullish this event is
bearish_score INT (0-100)         -- How bearish
source_quality_score INT (0-100)  -- Trust in the source
confidence_score INT (0-100)      -- LLM confidence in extraction
requires_manual_review BOOLEAN    -- Needs human review
```

### stock_scores
```sql
company_id BIGINT                  -- Which company
fundamental_score INT              -- Revenue, margin, cash flow
catalyst_score INT                 -- New products, guidance, partnerships
estimate_revision_score INT        -- Analyst estimate changes
valuation_score INT                -- P/E, Price/Sales vs growth
risk_score INT                     -- Debt, competition, valuation risk
momentum_score INT                 -- Price/volume momentum
source_quality_score INT           -- Quality of recent evidence
thesis_score INT                   -- How well thesis is holding
final_score INT                    -- Weighted combination (0-100)
```

### recommendations
```sql
company_id BIGINT                  -- Which company
action TEXT                        -- "BUY", "ADD", "HOLD", "TRIM", "SELL", "WATCH", "AVOID"
confidence_score INT               -- Recommendation confidence
reason TEXT                        -- Why this action
buy_trigger TEXT                   -- What would trigger a buy
sell_trigger TEXT                  -- What would trigger a sell
etf_comparison TEXT                -- How this compares to ETFs
```

## Scoring Formula

```
positive_score = 
  0.25 * catalyst +
  0.20 * fundamental +
  0.20 * estimates +
  0.15 * valuation +
  0.10 * source_quality +
  0.10 * momentum

risk_penalty = max(0, risk - 60) * 0.35
thesis_penalty = (thesis < 40) ? 10 : 0

final_score = clamp(positive_score - risk_penalty - thesis_penalty, 0, 100)
```

## Indexes

Key indexes for performance:

- `raw_documents.company_id` - Fast lookup by company
- `raw_documents.processing_status` - Job queue filtering
- `extracted_events.company_id` - Company events lookup
- `extracted_events.created_at DESC` - Recent events first
- `stock_scores.company_id` - Latest scores
- `document_embeddings.embedding` - pgvector similarity search

## Migrations

Migrations are managed by Flyway in `backend/src/main/resources/db/migration/`.

To add a new migration:
1. Create `V<number>__<description>.sql`
2. Flyway runs it automatically on app startup

---

**Version**: 1.0  
**pgvector Version**: 0.5.1+
