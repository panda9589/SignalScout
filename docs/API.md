# SignalScout API Contracts

## Base URL
```
http://localhost:8080/api
```

## Authentication
All endpoints (except `/auth/login`) require a JWT token in the `Authorization` header:
```
Authorization: Bearer {jwt_token}
```

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

### GET `/companies`
List all companies in the watchlist or system.

**Query Parameters:**
- `ticker` (optional): Filter by ticker
- `sector` (optional): Filter by sector

**Response:**
```json
[
  {
    "id": 1,
    "ticker": "MRVL",
    "name": "Marvell Technology",
    "sector": "Semiconductors",
    "currency": "CAD"
  }
]
```

### POST `/companies`
Create a new company.

**Request Body:**
```json
{
  "ticker": "MRVL",
  "name": "Marvell Technology",
  "sector": "Semiconductors",
  "currency": "CAD"
}
```

**Response:** 201 Created with Company object

### GET `/companies/{ticker}`
Get company details.

**Response:**
```json
{
  "id": 1,
  "ticker": "MRVL",
  "name": "Marvell Technology",
  "sector": "Semiconductors",
  "industry": "Semiconductor Manufacturing",
  "currency": "CAD",
  "latestScore": 75,
  "latestAction": "WATCH"
}
```

---

## Watchlist

### GET `/watchlist`
List all watchlist items for the user.

**Response:**
```json
[
  {
    "id": 1,
    "ticker": "MRVL",
    "companyName": "Marvell Technology",
    "priority": "high",
    "addedAt": "2026-06-01T10:00:00Z"
  }
]
```

### POST `/watchlist`
Add a company to the watchlist.

**Request Body:**
```json
{
  "ticker": "MRVL",
  "priority": "high",
  "notes": "Watching for data-center acceleration"
}
```

**Response:** 201 Created with Watchlist item

### DELETE `/watchlist/{ticker}`
Remove a company from the watchlist.

**Response:** 204 No Content

---

## Documents

### GET `/documents`
List all processed documents for the user.

**Query Parameters:**
- `ticker` (optional): Filter by company ticker
- `status` (optional): Filter by processing status (new, processing, completed, failed)

**Response:**
```json
[
  {
    "id": 1,
    "title": "Earnings Transcript Q2 2026",
    "sourceType": "MANUAL_PASTE",
    "ticker": "MRVL",
    "publishedAt": "2026-05-30T09:00:00Z",
    "status": "completed",
    "eventsCount": 3
  }
]
```

### POST `/documents/manual`
Upload a manual document (paste text or upload file).

**Request Body (multipart/form-data or JSON):**
```json
{
  "title": "Earnings Transcript Q2 2026",
  "rawText": "Good afternoon, everyone. Thank you for joining...",
  "ticker": "MRVL",
  "sourceUrl": "https://example.com/earnings-call"
}
```

**Response:** 201 Created with Document and extraction job reference

### GET `/documents/{id}`
Get document details and associated events.

**Response:**
```json
{
  "id": 1,
  "title": "Earnings Transcript",
  "rawText": "...",
  "ticker": "MRVL",
  "status": "completed",
  "events": [
    {
      "id": 1,
      "eventType": "earnings_release",
      "summary": "Q2 revenue beat expectations",
      "bullishScore": 75
    }
  ]
}
```

### POST `/documents/{id}/process`
Manually trigger AI extraction on a document.

**Response:** 202 Accepted (async job)

---

## Events

### GET `/events`
List all extracted investment events.

**Query Parameters:**
- `ticker` (optional): Filter by company
- `eventType` (optional): Filter by type (earnings_release, guidance_raise, etc.)

**Response:**
```json
[
  {
    "id": 1,
    "ticker": "MRVL",
    "eventType": "earnings_release",
    "summary": "Q2 revenue acceleration in data-center segment",
    "bullishScore": 75,
    "bearishScore": 30,
    "sourceQualityScore": 85,
    "confidenceScore": 78,
    "requiredManualReview": false
  }
]
```

### GET `/events/{id}`
Get event details with full bull/bear cases and evidence.

**Response:**
```json
{
  "id": 1,
  "ticker": "MRVL",
  "eventType": "earnings_release",
  "summary": "Q2 revenue acceleration in data-center segment",
  "bullCase": ["Revenue growth", "Margin improvement"],
  "bearCase": ["Valuation concerns"],
  "risks": ["Customer concentration"],
  "watchItems": ["Next earnings call", "Guidance"],
  "bullishScore": 75,
  "bearishScore": 30,
  "confidenceScore": 78
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
Generate on-demand portfolio action report.

**Response:** 201 Created with full portfolio action report

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

### GET `/portfolio/holdings`
List all holdings across accounts.

**Response:**
```json
[
  {
    "id": 1,
    "account": "TFSA",
    "ticker": "MRVL",
    "quantity": 100,
    "avgCost": 45.50,
    "marketValueCad": 5500,
    "portfolioWeight": 5.5
  }
]
```

### PUT `/portfolio/holdings/{id}`
Update a holding.

**Request Body:**
```json
{
  "quantity": 150,
  "avgCost": 45.50
}
```

---

## Theses

### GET `/theses`
List all investment theses.

**Response:**
```json
[
  {
    "id": 1,
    "ticker": "MRVL",
    "thesisText": "I believe AI data-center demand will drive MRVL custom silicon revenue...",
    "status": "active",
    "buyReason": "Custom silicon play on AI hyperscaler buildout",
    "timeHorizonMonths": 24
  }
]
```

### POST `/theses`
Create a new investment thesis.

**Request Body:**
```json
{
  "ticker": "MRVL",
  "thesisText": "AI data-center custom silicon thesis...",
  "buyReason": "Custom silicon demand from hyperscalers",
  "expectedTimeHorizonMonths": 24,
  "buyTriggers": ["Data-center revenue acceleration", "Guidance raise"],
  "sellTriggers": ["Guidance cut", "Customer loss"],
  "maxPositionPct": 5
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
