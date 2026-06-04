# SignalScout Phase 1 - Quick Reference Card

## 🚀 Start Here (5 Minutes)

### Prerequisites Check
```bash
java --version          # Should be 21+
gradle --version        # Should be 8.0+
psql --version         # Should be 14+
node --version         # Should be 18+
```

### Terminal 1: Start Database
```bash
createdb signalscout 2>/dev/null || true
psql signalscout < infra/postgres-init/001_init_schema.sql
```

### Terminal 2: Start Backend
```bash
cd backend
# Set your OpenAI API key before running:
export OPENAI_API_KEY=sk-...your-key-here...

gradle bootRun
# Wait for: "Started SignalScoutApplication"
# Backend ready at: http://localhost:8080/api
```

### Terminal 3: Start Frontend
```bash
cd frontend
npm install  # Only needed first time
npm run dev
# Frontend ready at: http://localhost:3000
```

---

## 📋 First Time Setup

1. **Open** http://localhost:3000/watchlist
2. **Click** "Initialize Sample Data" button
3. **Wait** for companies to appear (MRVL, AVGO, NVDA, MSFT, AMD)
4. **Click** on any company
5. **Paste** sample text:
   ```
   EARNINGS CALL - Q2 2026
   
   CEO: Strong quarter with revenue up 30% YoY to $15B.
   We're raising full-year guidance from $50B to $55B based on
   accelerating AI chip demand from major cloud providers.
   Gross margins improved 200bps to 52%.
   ```
6. **Click** "Extract Investment Event"
7. **View** AI-generated recommendation

---

## 🔌 API Quick Reference

### Extract Document
```bash
curl -X POST http://localhost:8080/api/documents/manual \
  -H "Content-Type: application/json" \
  -d '{
    "rawText": "Document text here...",
    "companyId": "1",
    "sourceType": "Manual"
  }'
```

### Get Company Detail
```bash
curl http://localhost:8080/api/companies/NVDA
```

### Get Watchlist
```bash
curl http://localhost:8080/api/companies/watchlist/summary
```

### Seed Data (one-time)
```bash
curl -X POST http://localhost:8080/api/admin/seed-companies
curl -X POST http://localhost:8080/api/admin/seed-themes
curl -X POST http://localhost:8080/api/admin/seed-sources
```

---

## 📊 Response Example

### Extract Document Response
```json
{
  "documentId": 1,
  "eventId": 1,
  "ticker": "NVDA",
  "companyName": "NVIDIA Corporation",
  "eventType": "guidance_raise",
  "summary": "Management raised FY2026 guidance by 15%",
  "bullishScore": 85,
  "bearishScore": 25,
  "sourceQualityScore": 95,
  "confidenceScore": 92,
  "stockScore": 82,
  "recommendation": "BUY",
  "bullCase": [
    "Strong AI demand accelerating",
    "Better than expected margins",
    "Conservative guidance historically"
  ],
  "bearCase": [
    "Valuation already high",
    "Competition from AMD increasing"
  ],
  "risks": [
    "Customer concentration (60% from 3 customers)",
    "Execution risk on new product lines",
    "Geopolitical headwinds on exports"
  ],
  "requiresManualReview": false,
  "createdAt": "2026-06-04T14:23:45"
}
```

---

## 🎯 Recommendation Logic

| Score | Recommendation | Meaning |
|-------|---|---|
| 0-39  | AVOID | Low quality or negative thesis |
| 40-59 | HOLD | Neutral, monitor for changes |
| 60-79 | WATCH | Interesting but needs confirmation |
| 80-100 | BUY | Strong signals, good entry point |

---

## 🗂️ Key Directories

```
backend/                # Java Spring Boot app
  src/main/java/       # Source code
  build.gradle         # Dependencies
  
frontend/              # Next.js React app
  app/                 # Pages and components
  lib/                 # Utilities and API client
  
infra/
  postgres-init/       # Database schema
  docker-compose/      # Local Postgres + Redis
  
docs/                  # Documentation
  EXTRACTION.md        # AI extraction schema
  DATABASE.md          # Database schema
  API.md              # API specification
```

---

## 🔧 Troubleshooting

### Backend won't start
```bash
# Check logs for errors
gradle bootRun 2>&1 | grep -i error

# Check database is running
psql signalscout -c "SELECT 1"

# Check port 8080 is free
lsof -i :8080  # Mac/Linux
netstat -ano | findstr :8080  # Windows
```

### Frontend can't connect to backend
```bash
# Check backend is running
curl http://localhost:8080/api/companies/watchlist/summary

# Check NEXT_PUBLIC_API_URL is correct
echo $NEXT_PUBLIC_API_URL
# Should output: http://localhost:8080/api
```

### Database error during extraction
```bash
# Verify tables exist
psql signalscout -c "\dt"

# Reinitialize schema
psql signalscout < infra/postgres-init/001_init_schema.sql
```

### OpenAI API errors
```bash
# Check API key is valid
echo $OPENAI_API_KEY
# Should start with: sk-

# Check API is reachable
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY" 2>&1 | head -20
```

---

## 📱 URL Navigation

| URL | Purpose |
|-----|---------|
| http://localhost:3000 | Home page |
| http://localhost:3000/watchlist | All companies |
| http://localhost:3000/company/NVDA | Company detail |
| http://localhost:8080/api/companies/watchlist/summary | API: Get all companies |
| http://localhost:8080/api/companies/NVDA | API: Get company detail |

---

## 💾 File Structure for Extraction

When you paste a document:

1. **RawDocument** table stores:
   - Raw text
   - Content hash (SHA256) for deduplication
   - Company ID
   - Source type
   - Processing status

2. **ExtractedEvent** table stores:
   - Event type (guidance_raise, earnings, partnership, etc.)
   - Scores (bullish, bearish, source quality, confidence)
   - Bull case, bear case, risks, watch items
   - Manual review flag if AI unsure

3. **Stock Score** computed from:
   - Bullish - bearish scores
   - Source quality adjustment
   - Formula: `(50 + (bull-bear)*0.5) * (quality/100)`

---

## 🎓 Understanding Scores

### Bullish Score (0-100)
How bullish is this event? (Higher = more positive)
- 80-100: Strong positive signals
- 50-79: Moderate positive
- 20-49: Mixed signals
- 0-19: Bearish signals

### Source Quality Score (0-100)
How trustworthy is the source?
- 90-100: SEC, official guidance
- 70-89: Earnings calls, earnings releases
- 50-69: News wire, analyst reports
- 30-49: Newsletter, general news
- 0-29: Social media, rumors

### Confidence Score (0-100)
How certain is the AI about the extraction?
- 80-100: Very confident
- 60-79: Mostly confident
- 40-59: Uncertain
- 0-39: Low confidence (requires manual review)

### Stock Score (0-100)
Final investment score for this event
- Combines bullish/bearish scores
- Adjusted for source quality
- Used to generate recommendation

---

## 🔄 Workflow Summary

```
1. User pastes document
   ↓
2. Backend receives request
   ↓
3. Hash content to check for duplicates
   ↓
4. Store raw document in PostgreSQL
   ↓
5. Call OpenAI GPT-4o with extraction schema
   ↓
6. Parse strict JSON response
   ↓
7. Store extracted event in PostgreSQL
   ↓
8. Compute stock score
   ↓
9. Generate recommendation (Buy/Watch/Hold/Avoid)
   ↓
10. Return result to frontend
   ↓
11. User sees extraction modal
   ↓
12. User closes modal
   ↓
13. Company detail page refreshes
   ↓
14. New event appears in history
```

---

## ✅ Success Checklist

- [ ] Backend starts without errors
- [ ] Frontend loads at http://localhost:3000
- [ ] Can click "Initialize Sample Data"
- [ ] See 5 companies appear in watchlist
- [ ] Can click on a company
- [ ] Can paste text in extraction form
- [ ] AI extraction completes in 3-7 seconds
- [ ] See stock score and recommendation
- [ ] Event appears in company history
- [ ] Can navigate back to watchlist
- [ ] See company now has a score

If all ✓, Phase 1 MVP is working correctly!

---

## 📞 Quick Help

| Problem | Solution |
|---------|----------|
| Nothing loads at localhost:3000 | Start frontend: `cd frontend && npm run dev` |
| API connection error | Start backend: `cd backend && gradle bootRun` |
| Database error | Reinit: `psql signalscout < infra/postgres-init/001_init_schema.sql` |
| OpenAI error | Check API key: `echo $OPENAI_API_KEY` should start with `sk-` |
| Extraction takes >10s | Check OpenAI API latency or increase timeout |
| "Company not found" | Make sure to click "Initialize Sample Data" first |
| Duplicate document error | Content already exists - try different text |

---

## 📖 Read Next

1. **PHASE1_MVP_GUIDE.md** - Complete setup instructions
2. **IMPLEMENTATION_SUMMARY.md** - What was built and why
3. **docs/EXTRACTION.md** - AI extraction schema details
4. **docs/API.md** - Full API documentation

---

**Version**: 1.0.0  
**Date**: 2026-06-04  
**Status**: Production Ready ✅
