
# Market Research Radar: AI-Assisted Investment Research Application

**Version:** 1.0  
**Date:** 2026-06-04  
**Primary user:** Canadian retail investor using CAD accounts, with interest in long-term research, watchlists, AI/semi/technology themes, and rule-based buy/hold/sell memos.  
**Core principle:** This application is a research radar and decision-support tool. It should not be an auto-trading bot and should not execute trades.

---

## 1. Executive Summary

Market Research Radar is a personal investing research assistant. It ingests official company documents, emails, news, transcripts, and manually pasted research; extracts structured events using an LLM; stores the raw documents and extracted signals; scores companies using deterministic rules; and generates a portfolio action report containing **Buy / Add / Hold / Trim / Sell / Watch / Avoid** recommendations.

The application is designed to catch the **ingredients** behind market-moving narratives earlier than a casual retail investor usually would. For example, instead of waiting for a headline like “MRVL could become a trillion-dollar company,” the app watches for upstream evidence: earnings-call language, data-center revenue acceleration, Nvidia/hyperscaler mentions, guidance changes, analyst estimate revisions, and official filings.

The app should not blindly turn every positive article into a buy recommendation. A high-quality output should say “do nothing” frequently. The goal is disciplined investing, not day-trading addiction.

**Main thesis:** AI is useful here because it can summarize, classify, compare, and extract signals from large amounts of text. The final buy/sell action should come from a rule engine that considers the user’s portfolio, risk limits, valuation, source quality, and thesis status.

---

## 2. Product Goals and Non-Goals

### 2.1 Goals

1. Ingest information from selected sources.
2. Normalize and deduplicate documents.
3. Extract structured investment-relevant events using AI.
4. Store raw sources, chunks, embeddings, extracted events, scores, and recommendations.
5. Compare new information against prior information to answer: **“What changed?”**
6. Maintain a watchlist of companies, tickers, sectors, and themes.
7. Maintain current portfolio holdings and user-defined risk rules.
8. Generate daily and weekly research digests.
9. Generate on-demand portfolio action reports.
10. Track the user’s original thesis for each stock and determine whether the thesis is stronger, weaker, unchanged, or broken.
11. Prevent hype-driven overtrading by requiring evidence, source quality, and risk checks.
12. Support Canadian-investor context: CAD base currency, TFSA/RRSP/non-registered account fields, FX awareness, and broad-ETF comparison.

### 2.2 Non-Goals

1. No automatic brokerage trading in version 1.
2. No promise of returns.
3. No attempt to beat professional market participants on speed.
4. No recommendation based only on social media hype.
5. No scraping paywalled or restricted content in a way that violates terms of service.
6. No public release as personalized financial-advice software without legal review.
7. No daily forced trading. The app should often recommend **Hold**, **Watch**, or **Do nothing**.

### 2.3 Success Criteria

The app is successful if it helps the user:

- Notice important company and industry changes earlier.
- Avoid buying based on one article or one social-media post.
- Maintain a disciplined watchlist.
- Compare individual-stock ideas against broad ETFs.
- Produce clear trade memos with evidence, bull case, bear case, triggers, and risk limits.
- Build a strong software portfolio project using Spring Boot, Postgres, AI, scheduled jobs, and a clean dashboard.

---

## 3. System Philosophy

### 3.1 AI Role

The LLM should be used for:

- Summarization.
- Event extraction.
- Bull/bear case extraction.
- Source classification.
- Ticker/company matching.
- Risk extraction.
- Comparing a new document to older documents.
- Drafting human-readable reports.

The LLM should **not** be the sole decision-maker for trade actions.

### 3.2 Rule Engine Role

The deterministic scoring and rule engine should handle:

- Final action classification: Buy / Add / Hold / Trim / Sell / Watch / Avoid.
- Position-size constraints.
- Maximum sector exposure.
- Minimum source-quality thresholds.
- Valuation-risk checks.
- Thesis-break detection.
- “Is this better than broad ETF?” checks.

### 3.3 Safety Rule

Every Buy/Add recommendation must answer:

> Why is this better than simply adding to a broad ETF such as XEQT, VEQT, VFV, XQQ, QQQ, BRK.B, or a CAD CDR equivalent, given the user’s goals and risk tolerance?

This reduces random single-stock chasing.

---

## 4. Recommended Tech Stack

### 4.1 MVP Stack

| Layer | Choice | Reason |
|---|---|---|
| Frontend | Next.js + React + Tailwind | Fast dashboard development and easy deployment. |
| Backend API | Java 21 + Spring Boot | Matches user’s work background and creates a strong backend portfolio project. |
| Worker | Spring Boot worker module initially | Keeps stack simple; Python worker can be added later if needed. |
| Database | Postgres | Reliable relational data store. |
| Vector Search | pgvector extension | Store embeddings in Postgres instead of managing a separate vector DB. |
| Cache/Queue | Redis optional | Add later for job queues/rate limits; not mandatory for MVP. |
| AI Provider | OpenAI API or equivalent LLM provider | Use structured JSON outputs for extraction. |
| Embeddings | OpenAI embeddings or equivalent | Store document-chunk embeddings for semantic search. |
| Object Storage | S3-compatible storage | Store raw HTML/PDF/text documents if needed. |
| Deployment | Docker + managed Postgres + simple container host | Avoid Kubernetes until the app deserves it. |
| CI/CD | GitHub Actions | Build, test, and deploy on push. |

### 4.2 Why Not Kubernetes First?

Kubernetes is useful later, but not for the first version. The MVP only needs:

- one API container,
- one worker container,
- one Postgres database,
- optionally one Redis instance,
- one frontend deployment.

Kubernetes adds operational burden before the core product is proven.

### 4.3 Suggested Repository Layout

```text
market-research-radar/
  README.md
  docs/
    design-document.md
    api-contracts.md
    prompts.md
    runbook.md
  infra/
    docker-compose.yml
    fly.toml or render.yaml or railway.json
    postgres-init/
      001_enable_pgvector.sql
  backend/
    build.gradle or pom.xml
    src/main/java/com/leotong/researchradar/
      api/
      auth/
      company/
      watchlist/
      document/
      ingestion/
      extraction/
      scoring/
      recommendation/
      report/
      portfolio/
      thesis/
      job/
      common/
  frontend/
    package.json
    app/
      dashboard/
      watchlist/
      company/[ticker]/
      reports/
      portfolio/
      settings/
  worker/
    optional separate Spring Boot module later
```

---

## 5. High-Level Architecture

### 5.1 Data Flow

```text
Sources
  - SEC EDGAR
  - Company investor relations emails
  - Gmail newsletters
  - RSS/news feeds
  - Earnings transcripts
  - Manual pasted articles
  - Future: Reddit/X/YouTube as low-trust smoke detectors

        ↓

Ingestion Jobs
  - Fetch documents
  - Normalize text
  - Extract metadata
  - Deduplicate with hash
  - Link to company/ticker/theme

        ↓

Storage
  - raw_documents
  - document_chunks
  - embeddings
  - source metadata

        ↓

AI Extraction
  - structured event JSON
  - bull/bear cases
  - risks
  - evidence snippets
  - what changed
  - watch items

        ↓

Scoring Engine
  - catalyst score
  - fundamental score
  - valuation score
  - risk score
  - source quality score
  - momentum score
  - final score

        ↓

Recommendation Engine
  - Buy / Add / Hold / Trim / Sell / Watch / Avoid
  - position sizing constraints
  - broad ETF comparison
  - thesis status check

        ↓

Outputs
  - dashboard
  - company page
  - daily digest
  - weekly report
  - on-demand portfolio action report
```

### 5.2 Services

1. **API Service**
   - Serves frontend.
   - Provides CRUD endpoints for watchlist, portfolio, theses, sources, reports.
   - Allows manual article upload/paste.
   - Triggers on-demand reports.

2. **Worker Service**
   - Runs scheduled ingestion jobs.
   - Processes unprocessed documents.
   - Calls AI extraction.
   - Computes scores.
   - Generates reports.

3. **Database**
   - Postgres with pgvector.
   - Stores both structured data and embeddings.

4. **Frontend**
   - Dashboard, watchlist, company page, reports, source settings, portfolio settings.

5. **AI Provider**
   - Text extraction.
   - Structured JSON outputs.
   - Embeddings.

---

## 6. Data Sources

### 6.1 SEC EDGAR

Purpose: Official filings and disclosures for U.S.-listed companies.

Initial filing types:

| Filing | Use |
|---|---|
| 8-K | Material events, earnings releases, corporate updates. |
| 10-Q | Quarterly financials and management discussion. |
| 10-K | Annual report, risks, business overview. |
| Form 4 | Insider buying/selling. |
| 13F | Institutional holdings, delayed but useful for idea tracking. |

Implementation notes:

- Store company CIK in the `companies` table.
- Poll watchlist companies every 1–2 hours.
- Compare accession numbers with previously stored filings.
- Only process new filings.
- Store filing date, accession number, form type, URL, title, raw text, and company.
- Use polite rate limiting and a clear User-Agent.

### 6.2 Gmail Ingestion

Purpose: Ingest investor-relations alerts, newsletters, earnings alerts, and saved articles from the user’s Gmail.

MVP approach:

- User manually creates Gmail labels:
  - `Investing/IR`
  - `Investing/Semi`
  - `Investing/AI`
  - `Investing/Earnings`
  - `Investing/Watchlist`
- App polls Gmail every 30 minutes for labeled unread/new messages.
- App stores sender, subject, sent date, body text, links, and attachments metadata.
- App does not delete emails.
- App can optionally mark processed messages with a label such as `Investing/Processed`.

Future approach:

- Use Gmail push notifications with Google Cloud Pub/Sub.
- Backend receives mailbox-change notifications.
- Backend fetches new messages from selected labels.

### 6.3 RSS and News Feeds

Purpose: Use trusted sources to detect and confirm market-moving discussion.

Initial source categories:

- Company investor-relations RSS feeds.
- News RSS feeds where allowed.
- Newsletter emails.
- Manual URL/text paste.

News should usually be confirmation, not the only source for a Buy recommendation.

### 6.4 Earnings Transcripts

Purpose: High-signal source for “what changed?”

Processing goals:

- Extract management tone.
- Compare with previous quarter.
- Detect repeated or new keywords.
- Detect guidance changes.
- Detect analyst question focus.
- Detect customer/partner/product mentions.
- Detect phrases like:
  - “AI revenue”
  - “data center”
  - “custom silicon”
  - “hyperscaler”
  - “design win”
  - “ramp”
  - “supply constrained”
  - “guidance”
  - “bookings”
  - “visibility”

### 6.5 Manual Paste / Upload

This is the MVP source and should be built first.

User flow:

1. User pastes article text or transcript excerpt.
2. User optionally specifies ticker/company/theme.
3. App stores raw document.
4. App extracts events.
5. App updates company page and report.

This lets the app become useful before automating source ingestion.

### 6.6 Future Low-Trust Sources

Social media, Reddit, X/Twitter, YouTube transcripts, and Discord exports should be treated as **smoke detectors** only.

Rules:

- Social-only evidence cannot produce a Buy recommendation.
- Social evidence must trigger verification against official filings, earnings calls, IR pages, or reliable news.
- The app should label social evidence as `LOW_TRUST`.

---

## 7. Core Data Model

### 7.1 Main Entities

- `users`
- `companies`
- `watchlist_items`
- `themes`
- `sources`
- `raw_documents`
- `document_chunks`
- `document_embeddings`
- `extracted_events`
- `event_evidence`
- `stock_scores`
- `recommendations`
- `portfolio_accounts`
- `portfolio_holdings`
- `investment_theses`
- `reports`
- `job_runs`
- `app_settings`

### 7.2 SQL Schema Draft

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email TEXT UNIQUE,
    display_name TEXT,
    base_currency TEXT NOT NULL DEFAULT 'CAD',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    ticker TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    cik TEXT,
    exchange TEXT,
    country TEXT,
    sector TEXT,
    industry TEXT,
    currency TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE themes (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE watchlist_items (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    priority TEXT NOT NULL DEFAULT 'normal',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(user_id, company_id)
);

CREATE TABLE company_themes (
    company_id BIGINT NOT NULL REFERENCES companies(id),
    theme_id BIGINT NOT NULL REFERENCES themes(id),
    PRIMARY KEY (company_id, theme_id)
);

CREATE TABLE sources (
    id BIGSERIAL PRIMARY KEY,
    source_type TEXT NOT NULL,
    name TEXT NOT NULL,
    base_url TEXT,
    trust_level TEXT NOT NULL DEFAULT 'medium',
    enabled BOOLEAN NOT NULL DEFAULT true,
    poll_interval_minutes INT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE raw_documents (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id),
    source_id BIGINT REFERENCES sources(id),
    source_type TEXT NOT NULL,
    external_id TEXT,
    source_url TEXT,
    title TEXT,
    author TEXT,
    published_at TIMESTAMP,
    retrieved_at TIMESTAMP NOT NULL DEFAULT now(),
    content_hash TEXT NOT NULL UNIQUE,
    raw_text TEXT NOT NULL,
    metadata_json JSONB,
    processing_status TEXT NOT NULL DEFAULT 'new'
);

CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES raw_documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    token_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(document_id, chunk_index)
);

CREATE TABLE document_embeddings (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL REFERENCES document_chunks(id) ON DELETE CASCADE,
    embedding vector(1536),
    model_name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE extracted_events (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES raw_documents(id) ON DELETE CASCADE,
    company_id BIGINT REFERENCES companies(id),
    event_type TEXT NOT NULL,
    event_date DATE,
    summary TEXT NOT NULL,
    what_changed TEXT,
    bull_case JSONB,
    bear_case JSONB,
    risks JSONB,
    watch_items JSONB,
    bullish_score INT CHECK (bullish_score BETWEEN 0 AND 100),
    bearish_score INT CHECK (bearish_score BETWEEN 0 AND 100),
    source_quality_score INT CHECK (source_quality_score BETWEEN 0 AND 100),
    confidence_score INT CHECK (confidence_score BETWEEN 0 AND 100),
    extraction_model TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE event_evidence (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES extracted_events(id) ON DELETE CASCADE,
    evidence_text TEXT NOT NULL,
    evidence_type TEXT,
    source_location TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE portfolio_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    account_name TEXT NOT NULL,
    account_type TEXT NOT NULL, -- TFSA, RRSP, non_registered, etc.
    base_currency TEXT NOT NULL DEFAULT 'CAD',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE portfolio_holdings (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES portfolio_accounts(id) ON DELETE CASCADE,
    company_id BIGINT REFERENCES companies(id),
    symbol TEXT NOT NULL,
    quantity NUMERIC(18,6),
    avg_cost NUMERIC(18,4),
    market_value_cad NUMERIC(18,2),
    portfolio_weight NUMERIC(8,4),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE investment_theses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    thesis_text TEXT NOT NULL,
    buy_reason TEXT,
    expected_time_horizon_months INT,
    buy_triggers JSONB,
    sell_triggers JSONB,
    invalidation_conditions JSONB,
    status TEXT NOT NULL DEFAULT 'active', -- active, stronger, weaker, broken, closed
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE stock_scores (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    fundamental_score INT,
    catalyst_score INT,
    estimate_revision_score INT,
    valuation_score INT,
    risk_score INT,
    momentum_score INT,
    source_quality_score INT,
    thesis_score INT,
    final_score INT,
    score_details JSONB,
    calculated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE recommendations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT REFERENCES companies(id),
    action TEXT NOT NULL, -- BUY, ADD, HOLD, TRIM, SELL, WATCH, AVOID, DO_NOTHING
    confidence_score INT,
    reason TEXT NOT NULL,
    buy_trigger TEXT,
    sell_trigger TEXT,
    suggested_position_size_pct NUMERIC(6,3),
    etf_comparison TEXT,
    risk_notes TEXT,
    source_event_ids JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    report_type TEXT NOT NULL, -- daily, weekly, action, company
    title TEXT NOT NULL,
    report_markdown TEXT NOT NULL,
    report_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE job_runs (
    id BIGSERIAL PRIMARY KEY,
    job_name TEXT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT now(),
    finished_at TIMESTAMP,
    status TEXT NOT NULL DEFAULT 'running',
    error_message TEXT,
    documents_found INT DEFAULT 0,
    documents_processed INT DEFAULT 0,
    ai_calls INT DEFAULT 0,
    estimated_cost_usd NUMERIC(12,4),
    metadata_json JSONB
);
```

---

## 8. AI Extraction Design

### 8.1 Extraction Inputs

For each document, provide the AI:

- document title,
- source type,
- source name,
- source date,
- company/ticker if known,
- document text or chunks,
- prior thesis if available,
- known watchlist themes.

### 8.2 Extraction Output Schema

Use strict JSON output. The schema should be versioned.

```json
{
  "schema_version": "1.0",
  "ticker": "MRVL",
  "company_name": "Marvell Technology",
  "document_relevance": "high",
  "event_type": "partnership_commentary",
  "event_date": "2026-06-04",
  "summary": "A concise summary of the important investment event.",
  "what_changed": "Explain what is new compared with prior known information.",
  "evidence": [
    {
      "text": "Paraphrased or short quoted evidence from the source.",
      "location": "paragraph 4 or section heading if available",
      "evidence_type": "management_commentary"
    }
  ],
  "bull_case": [
    "Why this could improve the company’s future fundamentals."
  ],
  "bear_case": [
    "Why this could be hype, already priced in, or not enough evidence."
  ],
  "risks": [
    "Valuation risk",
    "Customer concentration risk"
  ],
  "watch_items": [
    "Next earnings call",
    "Revenue guidance",
    "Segment revenue growth"
  ],
  "source_quality_score": 75,
  "bullish_score": 70,
  "bearish_score": 45,
  "confidence_score": 68,
  "requires_manual_review": true,
  "manual_review_reason": "Important claim is based on commentary and should be verified with earnings data."
}
```

### 8.3 Event Types

Use controlled values:

```text
earnings_release
guidance_raise
guidance_cut
product_launch
partnership
customer_win
customer_loss
management_commentary
analyst_upgrade
analyst_downgrade
estimate_revision
insider_buying
insider_selling
regulatory_risk
macro_theme
supply_constraint
margin_change
revenue_acceleration
valuation_warning
social_hype
unknown
```

### 8.4 Source Quality Ladder

| Source | Score Range | Notes |
|---|---:|---|
| SEC filing / official regulatory filing | 90–100 | Highest reliability for disclosed facts. |
| Company earnings call / official transcript / IR release | 80–95 | High reliability for management statements. |
| Reputable news wire / major financial publication | 65–85 | Good for confirmation and market context. |
| Analyst note summary | 55–80 | Useful but sometimes biased or incomplete. |
| Newsletter | 40–70 | Depends on author quality. |
| Reddit/X/social media | 10–45 | Smoke detector only. Cannot create Buy alone. |
| Unknown source | 0–30 | Manual review required. |

### 8.5 Extraction Prompt Template

```text
You are an investment research extraction engine for a Canadian retail investor.

Your task is to extract structured, evidence-based investment events from the provided document.
Do not recommend a trade directly. Only extract facts, changes, bull cases, bear cases, risks, and watch items.

Important rules:
1. Do not treat hype as proof.
2. Prefer official facts over commentary.
3. Separate confirmed facts from speculation.
4. If a claim needs verification, set requires_manual_review = true.
5. If the document is not relevant to the company/watchlist, mark relevance as low.
6. Return JSON only using the provided schema.

Document metadata:
- Title: {{title}}
- Source type: {{source_type}}
- Source name: {{source_name}}
- Published at: {{published_at}}
- Company/ticker hint: {{ticker_hint}}

Document text:
{{document_text}}
```

---

## 9. Scoring Engine

### 9.1 Score Categories

| Score | Meaning |
|---|---|
| Fundamental score | Revenue growth, margins, cash flow, balance sheet, profitability. |
| Catalyst score | New product, customer, partnership, guidance raise, industry boom. |
| Estimate revision score | Analysts or consensus estimates rising/falling. |
| Valuation score | Whether valuation is reasonable relative to growth and quality. |
| Risk score | Debt, competition, valuation excess, concentration, regulatory issues. Higher means more risk. |
| Momentum score | Price/volume trend, but not enough alone. |
| Source quality score | Reliability of the evidence. |
| Thesis score | Whether the user’s original thesis is stronger/weaker/broken. |

### 9.2 Final Score Formula

Initial formula:

```text
positive_score =
  0.25 * catalyst_score +
  0.20 * fundamental_score +
  0.20 * estimate_revision_score +
  0.15 * valuation_score +
  0.10 * source_quality_score +
  0.10 * momentum_score

risk_penalty = max(0, risk_score - 60) * 0.35
thesis_penalty = if thesis_score < 40 then 10 else 0

final_score = clamp(positive_score - risk_penalty - thesis_penalty, 0, 100)
```

### 9.3 Default Decision Rules

```text
BUY:
- final_score >= 80
- source_quality_score >= 70
- risk_score <= 60
- valuation_score >= 45
- catalyst or fundamental evidence is from official/reliable source
- position size after buy is within risk rules
- app explains why this is better than adding to broad ETF

ADD:
- already owned
- final_score >= 75
- thesis_score >= 60
- position size below target
- no major new risk

HOLD:
- already owned
- final_score between 55 and 80
- thesis intact or unclear
- no strong reason to add or sell

WATCH:
- final_score between 60 and 79
- catalyst is interesting but confirmation is missing
- buy trigger is clear

TRIM:
- position exceeds max weight
- valuation_score < 35 while final_score is not improving
- risk_score > 70
- stock has run far ahead of confirmed evidence

SELL:
- thesis is broken
- major guidance cut
- financials deteriorate materially
- risk_score > 80
- better alternative exists and opportunity cost is high

AVOID:
- source_quality_score < 50
- social hype only
- valuation extreme with no confirmed growth
- document relevance low

DO_NOTHING:
- no recommendation passes thresholds
- cash should stay unallocated or go to core ETF depending on portfolio settings
```

### 9.4 Position Sizing Rules

Default user settings:

```text
max_single_stock_position_pct = 10
max_aggressive_single_stock_position_pct = 5
max_sector_exposure_pct = 35
max_ai_semiconductor_exposure_pct = 25
min_source_quality_for_new_buy = 70
min_final_score_for_new_buy = 80
base_currency = CAD
require_etf_comparison = true
```

These should be editable in the UI.

### 9.5 Canadian Investor Rules

Fields to consider:

- Account type: TFSA, RRSP, FHSA, non-registered.
- Holding currency: CAD vs USD.
- FX conversion cost.
- Whether there is a CAD-listed equivalent ETF/CDR.
- Whether the stock pays dividends.
- Whether the user is over-concentrated in U.S. tech/AI.

The app should not automatically choose between USD shares and CAD CDRs. It should show a comparison memo:

```text
Direct USD stock:
- true underlying exposure
- FX conversion cost if CAD account
- unhedged CAD/USD movement

CAD CDR / CAD ETF:
- easier CAD purchase
- often hedged or partially hedged
- may have fees or hedge drag
```

---

## 10. Recommendation Output Format

### 10.1 On-Demand Portfolio Action Report

```text
Portfolio Action Report
Generated: {{timestamp}}

Summary:
- Overall action today: {{do nothing / rebalance / review watchlist / etc.}}
- Risk status: {{normal / concentrated / too speculative}}
- Cash deployment suggestion: {{none / core ETF / watchlist candidate}}

1. Buy
- Ticker: {{ticker}}
- Max position size: {{x}}%
- Confidence: {{score}}/100
- Why now:
- Evidence:
- Why better than broad ETF:
- Key risks:
- Buy trigger satisfied:
- Sell trigger:

2. Add
...

3. Hold
...

4. Trim
...

5. Sell
...

6. Watchlist
...

7. Do nothing
- Explanation

Manual checks before acting:
- Verify current price.
- Verify account type and FX cost.
- Verify position size.
- Read latest earnings release manually.
```

### 10.2 Company Page Report

Each company page should show:

- Ticker and company name.
- Current action.
- Current final score.
- Score history chart.
- Latest events.
- Thesis status.
- Bull case.
- Bear case.
- Buy triggers.
- Sell triggers.
- Relevant documents.
- Similar companies.
- Broad ETF comparison.

### 10.3 Weekly Report

```text
Weekly Market Intelligence Report

1. Executive summary
2. Biggest company-specific changes
3. Biggest theme/sector changes
4. Strongest positive signals
5. Strongest negative signals
6. Watchlist updates
7. Portfolio action recommendations
8. Upcoming events next week
9. What is probably hype
10. Manual research checklist
```

---

## 11. Thesis Tracking

### 11.1 Why Thesis Tracking Matters

When the user buys a stock, the app should require a thesis. This prevents emotional trading.

Example thesis:

```text
I bought MRVL because I believe AI data-center networking and custom silicon demand will grow over the next 4–8 quarters. I expect management to show data-center revenue acceleration and raise guidance. I accept high volatility but will sell or reduce if the AI revenue ramp fails to appear in reported numbers.
```

### 11.2 Thesis Status Rules

```text
STRONGER:
- New official evidence supports the thesis.
- Revenue/guidance/segment data improves.
- Customer/partner evidence becomes more concrete.

UNCHANGED:
- No material new evidence.
- News is mostly noise.

WEAKER:
- Evidence is delayed or less strong than expected.
- Management language becomes less confident.
- Competitors show better traction.

BROKEN:
- Guidance cut or thesis metric deteriorates materially.
- Key customer loss.
- Original expected catalyst fails repeatedly.
- Risk overwhelms upside.
```

### 11.3 Required Thesis Fields

```json
{
  "ticker": "MRVL",
  "thesis_text": "...",
  "buy_reason": "...",
  "time_horizon_months": 24,
  "expected_confirming_evidence": [
    "data-center revenue acceleration",
    "guidance raise",
    "customer/partner confirmation"
  ],
  "sell_triggers": [
    "guidance cut",
    "AI revenue fails to ramp after 2 quarters",
    "valuation becomes extreme without earnings support"
  ],
  "max_position_pct": 5
}
```

---

## 12. Backend API Design

### 12.1 REST Endpoints

```text
GET    /api/health
GET    /api/dashboard

GET    /api/companies
POST   /api/companies
GET    /api/companies/{ticker}
PUT    /api/companies/{ticker}

GET    /api/watchlist
POST   /api/watchlist
DELETE /api/watchlist/{ticker}

GET    /api/documents
POST   /api/documents/manual
GET    /api/documents/{id}
POST   /api/documents/{id}/process

GET    /api/events?company=MRVL
GET    /api/events/{id}

GET    /api/scores/{ticker}
POST   /api/scores/recompute

GET    /api/recommendations/latest
POST   /api/recommendations/generate
GET    /api/recommendations/{id}

GET    /api/portfolio/accounts
POST   /api/portfolio/accounts
GET    /api/portfolio/holdings
POST   /api/portfolio/holdings/import
PUT    /api/portfolio/holdings/{id}

GET    /api/theses
POST   /api/theses
PUT    /api/theses/{id}

GET    /api/reports
GET    /api/reports/{id}
POST   /api/reports/weekly/generate
POST   /api/reports/action/generate

GET    /api/jobs
GET    /api/jobs/{id}
POST   /api/jobs/{jobName}/run

GET    /api/settings
PUT    /api/settings
```

### 12.2 API Response Example: Recommendation

```json
{
  "ticker": "MRVL",
  "action": "WATCH",
  "confidenceScore": 68,
  "finalScore": 66,
  "reason": "Bullish AI/data-center commentary appeared, but confirmation from earnings and guidance is still needed.",
  "buyTrigger": "Buy only if next earnings shows data-center revenue acceleration and management raises guidance.",
  "sellTrigger": "Avoid or sell if guidance weakens or AI demand commentary disappears.",
  "suggestedPositionSizePct": 0,
  "riskNotes": "Valuation and hype risk remain high.",
  "etfComparison": "Currently not clearly better than adding to broad ETF exposure.",
  "manualChecks": [
    "Verify latest earnings release.",
    "Check valuation after price move.",
    "Confirm portfolio AI/semi concentration."
  ]
}
```

---

## 13. Frontend Design

### 13.1 Pages

1. **Dashboard**
   - Today’s action summary.
   - Top watchlist changes.
   - New documents processed.
   - Alerts requiring review.
   - AI cost and job status.

2. **Watchlist**
   - Ticker, company, sector, priority, latest score, latest action.
   - Filters by theme: AI, semiconductors, cybersecurity, energy, ETFs.

3. **Company Page**
   - Current score and action.
   - Timeline of extracted events.
   - Thesis status.
   - Bull/bear case.
   - Relevant docs.
   - Buy/sell triggers.

4. **Portfolio**
   - Accounts and holdings.
   - Position weights.
   - Sector/theme exposure.
   - CAD/USD split.
   - Risk-rule violations.

5. **Reports**
   - Daily digest.
   - Weekly report.
   - Action reports.
   - Export as Markdown/PDF later.

6. **Sources**
   - Add/edit source.
   - Enable/disable source.
   - Trust level.
   - Polling schedule.

7. **Settings**
   - Portfolio rules.
   - AI model settings.
   - Cost cap.
   - Notification preferences.

### 13.2 Dashboard Wireframe

```text
+-----------------------------------------------------+
| Market Research Radar                               |
| [Generate Action Report] [Add Manual Document]      |
+-----------------------------------------------------+
| Today: DO NOTHING / WATCHLIST REVIEW                |
| Risk: AI/Semi exposure high but within limit         |
+-------------------+-------------------------------+
| New Signals       | Portfolio Actions              |
| - MRVL WATCH      | Buy: None                      |
| - AVGO HOLD       | Add: Core ETF optional         |
| - VRT WATCH       | Trim: None                     |
+-------------------+-------------------------------+
| Latest Documents Processed                          |
| 1. MRVL earnings transcript                         |
| 2. Nvidia conference notes                          |
| 3. SEC 8-K: ...                                     |
+-----------------------------------------------------+
| Jobs / Health                                       |
| SEC: OK | Gmail: OK | RSS: OK | AI: 14 calls today  |
+-----------------------------------------------------+
```

---

## 14. Scheduled Jobs

### 14.1 Job List

| Job | Frequency | Purpose |
|---|---:|---|
| `SecFilingIngestionJob` | Every 1–2 hours | Check watchlist company filings. |
| `GmailIngestionJob` | Every 30 minutes | Pull labeled investing emails. |
| `RssIngestionJob` | Every 30–60 minutes | Pull configured feeds. |
| `DocumentChunkingJob` | Every 15 minutes | Chunk new documents. |
| `EmbeddingJob` | Every 30 minutes | Create embeddings for chunks. |
| `ExtractionJob` | Every 30 minutes | Run AI extraction on unprocessed docs. |
| `ScoreRecomputeJob` | Daily + on demand | Recompute scores. |
| `DailyDigestJob` | Daily evening | Create daily digest. |
| `WeeklyReportJob` | Weekly Sunday evening | Create weekly report. |
| `SourceHealthJob` | Daily | Check broken feeds/API failures. |
| `CostAuditJob` | Daily | Track AI calls and costs. |

### 14.2 Job Run Requirements

Every job must write to `job_runs`:

- job name,
- start time,
- finish time,
- status,
- documents found,
- documents processed,
- AI calls,
- estimated cost,
- error message if failed.

### 14.3 Failure Handling

- Failed jobs should be visible on dashboard.
- Repeated failures should disable a source after a threshold.
- AI extraction failures should preserve raw documents and allow manual retry.
- Duplicate documents should not be reprocessed.

---

## 15. Deployment Plan

### 15.1 Local Development

Use Docker Compose:

```yaml
version: "3.9"
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: radar
      POSTGRES_USER: radar
      POSTGRES_PASSWORD: radar_dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

### 15.2 MVP Cloud Deployment

Recommended simple deployment:

```text
Frontend: Vercel
Backend API: Render / Railway / Fly.io / AWS App Runner
Worker: same provider as backend, separate process/container
Database: managed Postgres with pgvector support
Storage: S3-compatible bucket
Secrets: provider environment variables
CI/CD: GitHub Actions
```

### 15.3 Environment Variables

```text
DATABASE_URL=
REDIS_URL=
OPENAI_API_KEY=
OPENAI_EXTRACTION_MODEL=
OPENAI_EMBEDDING_MODEL=
AI_DAILY_COST_LIMIT_USD=
GMAIL_CLIENT_ID=
GMAIL_CLIENT_SECRET=
GMAIL_REDIRECT_URI=
SEC_USER_AGENT=
S3_BUCKET=
S3_ACCESS_KEY=
S3_SECRET_KEY=
APP_BASE_URL=
JWT_SECRET=
```

### 15.4 Production Rules

- Never commit secrets.
- Log request IDs, not full private email contents.
- Encrypt OAuth tokens at rest.
- Back up Postgres daily.
- Set AI daily/monthly cost caps.
- Create admin endpoint to pause all ingestion jobs.

---

## 16. Security and Privacy

### 16.1 Sensitive Data

The app may ingest private emails and portfolio holdings. Treat these as sensitive.

Rules:

- Encrypt OAuth tokens at rest.
- Minimize stored personal data.
- Do not expose raw Gmail content publicly.
- Add auth before deployment beyond local machine.
- Separate dev and prod credentials.
- Do not log full document text by default.
- Allow delete/export of all user data.

### 16.2 Auth

MVP options:

1. Single-user local-only mode: simplest.
2. Email/password with Spring Security.
3. OAuth login later.

For personal use, single-user mode behind private login is enough.

### 16.3 Legal/Regulatory Guardrails

If the app is only for personal use, it is a research tool. If the app is shared with others and gives personalized buy/sell instructions, it may require legal/regulatory review. Therefore:

- Do not publicly market it as financial advice.
- Do not manage other people’s money through it.
- Do not automate trades for other users.
- Add disclaimers that outputs are research memos, not professional advice.

---

## 17. Testing Strategy

### 17.1 Unit Tests

Test:

- document hashing/deduplication,
- ticker matching,
- score formula,
- decision rules,
- position-size constraints,
- thesis-status transitions,
- prompt schema validation,
- JSON parser robustness.

### 17.2 Integration Tests

Test:

- manual document upload → extraction → event stored,
- SEC mock response → raw document stored,
- raw document → chunks → embeddings,
- events → scores → recommendation,
- portfolio rules block a Buy if position size is too high.

### 17.3 Golden Test Cases

Create a folder of sample documents and expected outputs.

Examples:

1. Positive official earnings release with guidance raise → possible Buy/Watch.
2. Social hype only → Avoid or Watch, never Buy.
3. Guidance cut → Sell/Trim risk warning.
4. Position already too large → Trim even if company is good.
5. Broad ETF better than single stock → recommend ETF/core addition instead.

### 17.4 Manual Review Tests

Before trusting the app, manually compare its weekly reports against actual source documents for at least 4 weeks.

---

## 18. Monitoring and Maintenance

### 18.1 Dashboard Health Indicators

Show:

- last SEC job success,
- last Gmail job success,
- last RSS job success,
- documents processed today,
- AI calls today,
- estimated AI cost today,
- failed documents,
- sources requiring attention.

### 18.2 Weekly Maintenance

- Check failed jobs.
- Check AI extraction errors.
- Review weird recommendations.
- Review duplicate documents.
- Remove low-quality sources.
- Confirm cost is under cap.

### 18.3 Monthly Maintenance

- Update watchlist.
- Review scoring weights.
- Add/remove sources.
- Review whether recommendation quality improved decisions.
- Back up database.

### 18.4 Quarterly Maintenance

- Compare the app’s thesis status and recommendations against earnings outcomes.
- Improve prompts and scoring weights.
- Archive stale documents.
- Review whether AI/semi concentration rules need changes.

---

## 19. Build Roadmap

### Phase 1: Manual MVP

Build first:

- Next.js dashboard skeleton.
- Spring Boot API.
- Postgres schema.
- Manual document paste endpoint.
- AI extraction into structured JSON.
- Extracted event display.
- Simple watchlist.
- Basic recommendation report.

Definition of done:

- User can paste an article/transcript.
- App extracts event, bull case, bear case, risks, watch items.
- App displays company page and action memo.

### Phase 2: Core Research Radar

Add:

- SEC EDGAR ingestion.
- RSS ingestion.
- Document deduping.
- Chunking and embeddings.
- Semantic search inside stored documents.
- Job dashboard.

Definition of done:

- App automatically detects new SEC filings for watchlist companies.
- App stores and processes official documents.
- App can search prior documents by meaning.

### Phase 3: Portfolio-Aware Decisions

Add:

- Portfolio accounts and holdings.
- Investment thesis tracking.
- Risk settings.
- Scoring engine.
- Buy/Add/Hold/Trim/Sell/Watch/Avoid rule engine.

Definition of done:

- App can generate a portfolio action report that respects position-size and sector limits.

### Phase 4: Reports and Workflow

Add:

- Daily digest.
- Weekly report.
- Upcoming events.
- Export to Markdown/PDF.
- Email report to self.

Definition of done:

- Every week, the app produces a clear report with actions, watchlist changes, hype warnings, and manual checks.

### Phase 5: Better Ingestion

Add:

- Gmail label ingestion.
- Gmail push notifications later.
- Earnings transcript source integration.
- Analyst estimate source if accessible.
- Optional low-trust social source monitoring.

Definition of done:

- App ingests user-selected emails/newsletters and processes them with source-quality rules.

### Phase 6: Evaluation and Backtesting

Add:

- Recommendation history.
- Outcome tracking.
- Manual rating of recommendation quality.
- Score-weight experiments.

Definition of done:

- User can review whether the app’s “Watch/Buy/Sell” calls were reasonable over time.

---

## 20. Example Prompt for Future AI Coding Agent

Use this when moving the project to another ChatGPT/AI service:

```text
You are helping me build an application called Market Research Radar.
Use the attached design document as the source of truth.

Build the app in phases. Do not skip phase 1.

Tech stack:
- Backend: Java 21, Spring Boot, Postgres, pgvector
- Frontend: Next.js, React, Tailwind
- AI: structured JSON extraction from documents, embeddings for search
- Deployment: Docker, managed Postgres, simple container host, GitHub Actions

Core rule:
The AI extracts facts and drafts memos. The deterministic scoring/rule engine decides Buy/Add/Hold/Trim/Sell/Watch/Avoid.
No brokerage execution.

Phase 1 task:
Create a working MVP where I can paste a document, tag it with a ticker, run AI extraction into a strict JSON schema, store the extracted event in Postgres, and view it on a company page.

Do not implement SEC/Gmail automation until the manual MVP works.

Generate code with tests and include setup instructions.
```

---

## 21. Open Questions for Implementation

1. Which cloud provider should be used first: Render, Railway, Fly.io, AWS, or local-only?
2. Which AI provider/model should be used for extraction vs report generation?
3. How many stocks should be in the initial watchlist?
4. Should portfolio holdings be entered manually or imported from CSV?
5. Should Gmail ingestion be added before or after SEC ingestion?
6. What exact account types should be supported first: TFSA, RRSP, non-registered?
7. Should the app support Canadian tickers and CAD-listed ETFs in phase 1 or phase 2?
8. How conservative should the Buy threshold be?
9. Should recommendations expire after a certain number of days?
10. Should user be forced to enter a thesis before any single-stock Buy recommendation is allowed?

Recommended answers for MVP:

- Local Docker + managed Postgres later.
- Manual portfolio entry first.
- 20-stock watchlist maximum.
- SEC before Gmail.
- Canadian account fields from the start.
- Buy threshold high: 80+.
- Recommendations expire after 7 days unless refreshed.
- Thesis required before single-stock Buy.

---

## 22. References and Source Notes

These references are used for implementation planning and should be checked again before coding against the APIs:

1. SEC EDGAR APIs: official SEC developer/API pages describe REST APIs for company submissions and XBRL financial data via `data.sec.gov`. URL: https://www.sec.gov/search-filings/edgar-application-programming-interfaces
2. Gmail API push notifications: official Google documentation describes Gmail mailbox watches and Pub/Sub-based push notifications. URL: https://developers.google.com/workspace/gmail/api/guides/push
3. OpenAI Structured Outputs: official OpenAI documentation describes constraining model output to JSON schemas. URL: https://developers.openai.com/api/docs/guides/structured-outputs
4. OpenAI Embeddings: official OpenAI documentation describes embedding models for semantic search. URL: https://developers.openai.com/api/docs/guides/embeddings
5. pgvector: official project documentation describes vector similarity search inside Postgres. URL: https://github.com/pgvector/pgvector

---

## 23. Final Product Definition

The finished version should be able to answer this prompt:

```text
Generate my portfolio action report.
Tell me what changed this week, what I should buy/add/hold/trim/sell/watch/avoid, what evidence supports it, what is hype, what manual checks I need to do, and whether each idea is better than just adding to a broad ETF.
```

A good answer from the app should look like:

```text
Buy: None
Add: Core ETF optional if deploying new cash
Hold: Existing high-quality positions where thesis is intact
Trim: Any holding above max position size or with thesis weakening
Sell: Only if thesis is broken or risk changed materially
Watch: Companies with promising catalysts but missing confirmation
Avoid: Social-media hype without official evidence
Manual checks: latest earnings, valuation, FX/account cost, position size
```

That behavior is the core of the project.
