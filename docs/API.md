# SignalScout API Contracts

## Base URL
```
http://localhost:8080/api
```

## Authentication
Local MVP endpoints are currently open in development. Add JWT enforcement before production.

---

## Health & Info

### GET `/health`
Check application health.

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2026-06-04T10:00:00Z"
}
```

---

## Companies

### GET `/companies/watchlist/summary`
Get all seeded watchlist companies with latest score/recommendation summary.

**Response:**
```json
[
  {
    "companyId": 3,
    "ticker": "NVDA",
    "name": "NVIDIA Corporation",
    "sector": "Semiconductors",
    "latestStockScore": 33,
    "latestRecommendation": "AVOID",
    "recentEventCount": 1
  }
]
```

### GET `/companies/{ticker}`
Get company details.

**Response:**
```json
{
  "company": {
    "id": 3,
    "ticker": "NVDA",
    "name": "NVIDIA Corporation",
    "sector": "Semiconductors",
    "industry": "Semiconductors",
    "currency": "USD"
  },
  "latestStockScore": 33,
  "latestRecommendation": "AVOID",
  "recentEvents": []
}
```

---

## Documents

### POST `/documents/manual`
Upload a manual document as JSON. This stores the raw document, chunks it, runs extraction, stores the extracted event, and returns the score.

**Request Body:**
```json
{
  "raw_text": "Good afternoon, everyone. Thank you for joining...",
  "company_id": "3",
  "source_type": "MANUAL_PASTE"
}
```

**Response:**
```json
{
  "documentId": 3,
  "eventId": 3,
  "ticker": "NVDA",
  "companyName": "NVIDIA Corporation",
  "eventType": "management_commentary",
  "summary": "Mock extraction - OpenAI API not configured.",
  "stockScore": 33,
  "recommendation": "AVOID"
}
```

### POST `/documents/{id}/chunk`
Recreate chunks for an existing raw document and record a `DocumentChunkingJob`.

**Response:**
```json
{
  "documentId": 1,
  "jobRunId": 1,
  "chunksCreated": 1
}
```

### GET `/documents/{id}/chunks`
List chunks for a stored document.

**Response:**
```json
[
  {
    "id": 1,
    "documentId": 3,
    "chunkIndex": 0,
    "chunkText": "NVIDIA reported accelerating AI platform demand...",
    "tokenCount": 80,
    "createdAt": "2026-06-04T18:10:00.614615"
  }
]
```

### GET `/documents/search`
Search stored chunks by keyword.

**Query Parameters:**
- `q` (required): Search text
- `limit` (optional, default `10`, max `50`): Number of results

**Response:**
```json
[
  {
    "documentId": 3,
    "chunkId": 1,
    "chunkIndex": 0,
    "ticker": "NVDA",
    "title": "Manual Paste - 2026-06-04T18:10:00.578070400",
    "sourceType": "MANUAL_PASTE",
    "snippet": "NVIDIA reported accelerating AI platform demand..."
  }
]
```

### GET `/documents/semantic-search`
Search stored chunks by vector similarity. Requires embeddings to exist and `OPENAI_API_KEY` to be set so the query can be embedded.

**Query Parameters:**
- `q` (required): Search text
- `limit` (optional, default `10`, max `50`): Number of results

**Response:**
```json
[
  {
    "documentId": 4,
    "chunkId": 12,
    "chunkIndex": 8,
    "ticker": "NVDA",
    "title": "10-Q - 2026-05-20 - 10-Q",
    "sourceType": "SEC_FILING",
    "snippet": "Revenue from data center compute...",
    "distance": 0.2142
  }
]
```

---

## Embeddings

### POST `/embeddings/run`
Generate missing OpenAI embeddings for stored document chunks and save them to `document_embeddings`.

**Query Parameters:**
- `limit` (optional, default `100`, max `500`): Number of chunks to embed.

**Response:**
```json
{
  "jobRunId": 4,
  "chunksFound": 58,
  "embeddingsCreated": 58,
  "aiCalls": 2,
  "modelName": "text-embedding-3-small"
}
```

Requires `OPENAI_API_KEY`.

---

## Scheduled Ingestion

Scheduled ingestion is available but disabled by default.

```text
INGESTION_SCHEDULER_ENABLED=true
INGESTION_SEC_LIMIT_PER_COMPANY=3
INGESTION_RSS_FEEDS=https://example.com/feed.xml|NVDA,https://example.com/macro.xml
INGESTION_RSS_LIMIT_PER_FEED=5
SEC_USER_AGENT=SignalScout your-email@example.com
```

When enabled, the scheduler runs SEC EDGAR ingestion for all companies with CIKs and RSS ingestion for each configured feed. RSS feed entries use `feedUrl|TICKER`; omit `|TICKER` for unassigned macro/news feeds.

Config templates live in:

- `config/ingestion.local.env.example`: dummy local values with scheduler disabled.
- `config/ingestion.prod.env.example`: deployment-oriented values with scheduler enabled.

For local development, copy the local example to `config/ingestion.local.env`. The backend dev script loads it automatically.

---

## Jobs

### GET `/jobs`
List recent job runs.

**Query Parameters:**
- `limit` (optional, default `20`, max `100`): Number of jobs

**Response:**
```json
[
  {
    "id": 1,
    "jobName": "DocumentChunkingJob",
    "startedAt": "2026-06-04T18:10:18.422131",
    "finishedAt": "2026-06-04T18:10:18.442889",
    "status": "success",
    "errorMessage": null,
    "documentsFound": 1,
    "documentsProcessed": 1,
    "aiCalls": 0,
    "estimatedCostUsd": null
  }
]
```

### GET `/jobs/{id}`
Get a single job run.

**Response:**
```json
{
  "id": 7,
  "jobName": "EmbeddingJob",
  "startedAt": "2026-06-04T20:50:00",
  "finishedAt": "2026-06-04T20:50:03",
  "status": "success",
  "documentsFound": 56,
  "documentsProcessed": 56,
  "aiCalls": 2
}
```

---

## Ingestion

### POST `/ingestion/sec/run`
Fetch recent SEC EDGAR filings from `data.sec.gov` for one ticker or all companies with CIKs, store new filings as raw documents, create chunks, and record a job run.

**Query Parameters:**
- `ticker` (optional): Company ticker to ingest. If omitted, scans all companies with CIKs.
- `limit` (optional, default `3`, max `20`): Recent matching filings per company.

**Response:**
```json
{
  "jobRunId": 2,
  "companiesScanned": 1,
  "filingsFound": 1,
  "documentsStored": 1,
  "duplicatesSkipped": 0,
  "chunksCreated": 58
}
```

Set `SEC_USER_AGENT` before production use so SEC requests identify your app and contact email.

### POST `/ingestion/sec/documents/{documentId}/reprocess`
Re-download an already-stored SEC filing, apply the current SEC text cleanup, and recreate chunks. Existing embeddings for that document's old chunks are deleted by cascade; run `/embeddings/run` after reprocessing.

**Response:** same shape as `/ingestion/sec/run`.

### POST `/ingestion/rss/run`
Fetch RSS or Atom feed items, store new items as raw documents, create chunks, and record a job run.

**Query Parameters:**
- `feedUrl` (required): RSS or Atom feed URL.
- `ticker` (optional): Existing company ticker to associate with the feed items.
- `limit` (optional, default `5`, max `50`): Feed items to process.

**Response:**
```json
{
  "jobRunId": 5,
  "itemsFound": 5,
  "documentsStored": 5,
  "duplicatesSkipped": 0,
  "autoMatchedDocuments": 3,
  "chunksCreated": 5
}
```

---

## Scores & Recommendations

### GET `/scores/{ticker}`
Get the latest score for a company.

**Response:**
```json
{
  "ticker": "MRVL",
  "fundamentalScore": 70,
  "catalystScore": 80,
  "estimateRevisionScore": 75,
  "valuationScore": 60,
  "riskScore": 45,
  "momentumScore": 65,
  "sourceQualityScore": 80,
  "finalScore": 72,
  "calculatedAt": "2026-06-04T09:00:00Z"
}
```

### POST `/scores/recompute`
Manually recompute all scores.

**Response:** 202 Accepted (async job)

### GET `/recommendations/latest`
Get the latest portfolio action recommendations.

**Query Parameters:**
- `limit` (optional, default 10): Number of recommendations

**Response:**
```json
[
  {
    "id": 1,
    "ticker": "MRVL",
    "action": "WATCH",
    "confidenceScore": 75,
    "reason": "Bullish AI/data-center commentary but needs earnings confirmation",
    "buyTrigger": "Next earnings shows data-center acceleration",
    "sellTrigger": "AI commentary disappears or margins compress",
    "suggestedPositionSizePct": 0,
    "etfComparison": "Not clearly better than VFV + QQQ at current valuation",
    "createdAt": "2026-06-04T09:00:00Z"
  }
]
```

### POST `/recommendations/generate`
Generate an on-demand portfolio action report from current holdings, active theses, risk settings, and latest extracted company events.

**Response:**
```json
{
  "generatedAt": "2026-06-04T23:50:00",
  "totalMarketValueCad": 1000,
  "riskSettings": {
    "maxSingleStockPositionPct": 12,
    "maxSectorExposurePct": 35,
    "minScoreForNewBuy": 72,
    "minSourceQualityForNewBuy": 65
  },
  "actions": [
    {
      "companyId": 3,
      "ticker": "NVDA",
      "companyName": "NVIDIA Corporation",
      "sector": "Semiconductors",
      "action": "HOLD",
      "stockScore": 68,
      "currentWeightPct": 100,
      "sectorExposurePct": 100,
      "hasHolding": true,
      "hasActiveThesis": true,
      "reason": "Position is above the configured max single-stock limit."
    }
  ]
}
```

---

## Portfolio

### GET `/portfolio/accounts`
List user's portfolio accounts.

**Response:**
```json
[
  {
    "id": 1,
    "accountName": "TFSA",
    "accountType": "TFSA",
    "baseCurrency": "CAD"
  }
]
```

### POST `/portfolio/accounts`
Create a new portfolio account.

**Request Body:**
```json
{
  "accountName": "RRSP",
  "accountType": "RRSP",
  "baseCurrency": "CAD"
}
```

### PUT `/portfolio/accounts/{id}`
Update an existing portfolio account.

**Request Body:** same shape as `POST /portfolio/accounts`.

### DELETE `/portfolio/accounts/{id}`
Delete an account and its holdings.

### GET `/portfolio/holdings`
List all holdings across accounts.

**Response:**
```json
[
  {
    "id": 1,
    "accountId": 1,
    "accountName": "TFSA",
    "ticker": "MRVL",
    "symbol": "MRVL",
    "quantity": 100,
    "avgCost": 45.50,
    "marketValueCad": 5500,
    "portfolioWeight": 5.5
  }
]
```

### POST `/portfolio/holdings`
Create a holding entry.

**Request Body:**
```json
{
  "accountId": 1,
  "symbol": "NVDA",
  "quantity": 1,
  "avgCost": 900,
  "marketValueCad": 1000
}
```

### PUT `/portfolio/holdings/{id}`
Update a holding entry.

**Request Body:** same shape as `POST /portfolio/holdings`.

### DELETE `/portfolio/holdings/{id}`
Delete a holding entry.

### GET `/portfolio/risk-settings`
Get portfolio action thresholds.

### PUT `/portfolio/risk-settings`
Update portfolio action thresholds.

**Request Body:**
```json
{
  "maxSingleStockPositionPct": 12,
  "maxSectorExposurePct": 35,
  "minScoreForNewBuy": 72,
  "minSourceQualityForNewBuy": 65
}
```

### PUT `/theses/{id}`
Update an investment thesis.

**Request Body:** same shape as `POST /theses`.

### DELETE `/theses/{id}`
Delete an investment thesis.

---

## Theses

### GET `/theses`
List all investment theses.

**Response:**
```json
[
  {
    "id": 1,
    "companyId": 1,
    "ticker": "MRVL",
    "companyName": "Marvell Technology, Inc.",
    "thesisText": "I believe AI data-center demand will drive MRVL custom silicon revenue...",
    "status": "active",
    "buyReason": "Custom silicon play on AI hyperscaler buildout",
    "expectedTimeHorizonMonths": 24
  }
]
```

### POST `/theses`
Create a new investment thesis.

**Request Body:**
```json
{
  "companyId": 1,
  "thesisText": "AI data-center custom silicon thesis...",
  "buyReason": "Custom silicon demand from hyperscalers",
  "expectedTimeHorizonMonths": 24,
  "status": "ACTIVE"
}
```

---

## Reports

### GET `/reports`
List all generated reports.

**Query Parameters:**
- `reportType` (optional): daily, weekly, action, company

**Response:**
```json
[
  {
    "id": 1,
    "reportType": "weekly",
    "title": "Weekly Signal Report - Week of 2026-06-02",
    "createdAt": "2026-06-08T18:00:00Z"
  }
]
```

### GET `/reports/{id}`
Get report full content.

**Response:**
```json
{
  "id": 1,
  "reportType": "weekly",
  "title": "Weekly Signal Report",
  "reportMarkdown": "# Weekly Report...",
  "createdAt": "2026-06-08T18:00:00Z"
}
```

---

## Settings

### GET `/settings`
Get user settings.

**Response:**
```json
{
  "maxSingleStockPositionPct": 10,
  "maxSectorExposurePct": 35,
  "minSourceQualityForNewBuy": 70,
  "minFinalScoreForNewBuy": 80,
  "requireEtfComparison": true
}
```

### PUT `/settings`
Update user settings.

**Request Body:**
```json
{
  "maxSingleStockPositionPct": 7,
  "minFinalScoreForNewBuy": 75
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "timestamp": "2026-06-04T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid ticker format"
}
```

### Common Status Codes
- `200 OK`: Success
- `201 Created`: Resource created
- `204 No Content`: Success with no response body
- `400 Bad Request`: Invalid input
- `401 Unauthorized`: Missing or invalid JWT
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error

---

**API Version**: 1.0  
**Last Updated**: 2026-06-04
