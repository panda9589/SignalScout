# SignalScout Extraction Schema Reference

## JSON Schema for AI Extraction Output

All LLM extraction outputs must conform to this strict schema:

```json
{
  "schema_version": "1.0",
  "ticker": "MRVL",
  "company_name": "Marvell Technology",
  "document_relevance": "high",
  "event_type": "partnership_commentary",
  "event_date": "2026-06-04",
  "summary": "A concise summary of the important investment event in 1-2 sentences.",
  "what_changed": "Explain what is new compared with prior known information. Why does this matter?",
  "evidence": [
    {
      "text": "Paraphrased or short quoted evidence from the source.",
      "location": "paragraph 4 or section heading if available",
      "evidence_type": "management_commentary"
    },
    {
      "text": "Additional supporting evidence.",
      "location": "earnings call slide 15",
      "evidence_type": "official_guidance"
    }
  ],
  "bull_case": [
    "Why this could improve the company's future fundamentals.",
    "How this validates investor thesis.",
    "Potential upside scenarios."
  ],
  "bear_case": [
    "Why this could be hype or already priced in.",
    "Alternative explanations or risks.",
    "What would need to happen to invalidate bullish view."
  ],
  "risks": [
    "Valuation risk: stock price has run ahead of earnings",
    "Customer concentration risk: top 3 customers = 60% of revenue",
    "Execution risk: historical delays on product launches"
  ],
  "watch_items": [
    "Next earnings call for revenue growth confirmation",
    "Segment revenue breakdown in guidance",
    "Management commentary on design wins"
  ],
  "source_quality_score": 75,
  "bullish_score": 70,
  "bearish_score": 45,
  "confidence_score": 68,
  "requires_manual_review": true,
  "manual_review_reason": "Important claim about partnership scope is based on management commentary and should be verified with official press release."
}
```

## Field Definitions

| Field | Type | Range | Meaning |
|-------|------|-------|---------|
| `schema_version` | string | - | Version of this schema (for tracking) |
| `ticker` | string | - | Company ticker or "UNKNOWN" if not identifiable |
| `company_name` | string | - | Full company name |
| `document_relevance` | string | high, medium, low | How relevant this document is to the company |
| `event_type` | string | See list below | Category of the event |
| `event_date` | date | YYYY-MM-DD | When the event occurred |
| `summary` | string | 1-2 sentences | Executive summary of the event |
| `what_changed` | string | Free text | What is materially new vs prior information |
| `evidence` | array | - | Array of evidence objects with text, location, type |
| `bull_case` | array | 1-5 items | Bullish arguments for this event |
| `bear_case` | array | 1-5 items | Bearish arguments or caution |
| `risks` | array | 1-5 items | Key risks to consider |
| `watch_items` | array | 1-5 items | What to monitor next |
| `source_quality_score` | int | 0-100 | Trust in the source (see ladder below) |
| `bullish_score` | int | 0-100 | How bullish is this event |
| `bearish_score` | int | 0-100 | How bearish/cautionary is this |
| `confidence_score` | int | 0-100 | LLM confidence in extraction accuracy |
| `requires_manual_review` | bool | - | If true, human should verify |
| `manual_review_reason` | string | - | Why manual review is needed |

## Event Types

```
earnings_release          - Official earnings report
guidance_raise           - Management raises guidance
guidance_cut             - Management cuts guidance
product_launch           - New product announced
partnership              - New partnership or joint venture
customer_win             - Major customer win or order
customer_loss            - Customer lost or reduced order
management_commentary    - General management statements
analyst_upgrade          - Analyst raises rating/target
analyst_downgrade        - Analyst lowers rating/target
estimate_revision        - Consensus estimates changed
insider_buying           - Insider purchases shares
insider_selling          - Insider sells shares
regulatory_risk          - Regulatory or legal issue
macro_theme              - Macro trend (AI boom, supply chain, etc)
supply_constraint        - Supply chain issue
margin_change            - Gross/operating margin change
revenue_acceleration     - Faster revenue growth than expected
valuation_warning        - Valuation is getting stretched
social_hype              - Social media chatter (low confidence)
unknown                  - Unclear or other
```

## Source Quality Ladder

```
SEC Filing / Official Regulatory           90-100
Earnings Call Transcript                   80-95
Company IR Release                         80-90
Reputable News Wire (Reuters, Bloomberg)   65-85
Financial Magazine or Report               60-80
Analyst Note (if from reputable firm)      55-80
Newsletter (depends on author)             40-70
Reddit / X / Social Media                  10-45
Unknown Source                             0-30
```

## Extraction Rules

1. **Do not treat hype as proof**: Social media excitement alone does not make bullish evidence.
2. **Prefer official facts over commentary**: SEC filings > earnings calls > news articles > social media.
3. **Separate confirmed facts from speculation**: "We are investing in AI" is different from "We launched an AI revenue stream."
4. **If a claim needs verification, flag it**: Set `requires_manual_review = true` and explain why.
5. **If document is not relevant to the company**, set `document_relevance = "low"` and return early.
6. **Confidence score reflects extraction accuracy**, not event bullishness. A clear bearish event should have high confidence.
7. **Bull case and bear case should be balanced**: Both should be 2-4 points each, even for very positive or negative news.

## Example Extractions

### Example 1: Earnings Release with Guidance Raise

```json
{
  "schema_version": "1.0",
  "ticker": "MRVL",
  "company_name": "Marvell Technology",
  "document_relevance": "high",
  "event_type": "earnings_release",
  "event_date": "2026-06-03",
  "summary": "Marvell reported Q2 FY26 revenue of $1.5B (+18% YoY) with data-center segment up 35%, prompting management to raise full-year revenue guidance by 8%.",
  "what_changed": "Data-center revenue acceleration is stronger than prior guidance assumed. Management now expects full-year revenue of $6.2B (up from $5.74B). This validates the AI/hyperscaler thesis.",
  "evidence": [
    {
      "text": "Data-center revenue was $550M, up 35% year-over-year, driven by custom silicon design wins with hyperscalers.",
      "location": "Earnings release, revenue segment breakdown",
      "evidence_type": "official_guidance"
    },
    {
      "text": "CFO stated: 'We're seeing sustained demand for our AI-optimized networking and custom silicon solutions across major cloud providers.'",
      "location": "Earnings call transcript, CFO remarks",
      "evidence_type": "management_commentary"
    }
  ],
  "bull_case": [
    "Data-center segment growth of 35% validates AI hyperscaler investment thesis",
    "Management confidence warranted by raising full-year guidance by 8%",
    "Recurring revenue from custom silicon provides higher-margin, stickier business",
    "Market opportunity for AI chips remains large and underpenetrated"
  ],
  "bear_case": [
    "Stock has already run +40% YTD; event may already be priced in",
    "Hyperscaler custom silicon programs are typically 2-3 year efforts; sustainability uncertain",
    "Competition from AMD, Broadcom, and in-house hyperscaler chips remains intense"
  ],
  "risks": [
    "Valuation risk: P/E now ~35x vs industry 20x; leaves little room for disappointment",
    "Customer concentration: top 3 hyperscaler customers likely represent 60%+ of data-center revenue",
    "Execution risk: any delay in next-gen custom silicon launch could unwind momentum"
  ],
  "watch_items": [
    "Q3 guidance and management commentary on data-center pipeline",
    "Design win announcements from specific hyperscalers",
    "Competitive positioning vs. AMD's custom silicon efforts",
    "Free cash flow generation given rising CapEx for manufacturing"
  ],
  "source_quality_score": 95,
  "bullish_score": 78,
  "bearish_score": 38,
  "confidence_score": 92,
  "requires_manual_review": false,
  "manual_review_reason": null
}
```

### Example 2: Social Media Hype (Low Confidence)

```json
{
  "schema_version": "1.0",
  "ticker": "XYZ",
  "company_name": "Unknown Company",
  "document_relevance": "low",
  "event_type": "social_hype",
  "event_date": "2026-06-04",
  "summary": "Reddit and X users posting about XYZ's upcoming AI announcement; no official company statement yet.",
  "what_changed": "Social media chatter has increased 5x in past 2 days; unclear what the actual announcement will be.",
  "evidence": [
    {
      "text": "r/investing post: 'XYZ is about to become a trillion-dollar company with their AI breakthrough'",
      "location": "Reddit r/investing, 2k upvotes",
      "evidence_type": "social_chatter"
    }
  ],
  "bull_case": [
    "If AI announcement is material, could be a major catalyst"
  ],
  "bear_case": [
    "No official confirmation yet; this could be baseless speculation",
    "Reddit/Twitter sentiment often precedes disappointment",
    "Retail trading hype rarely correlates with fundamental value creation"
  ],
  "risks": [
    "Execution risk: What actually happens may differ from speculation",
    "Valuation risk: Stock may run on hype, creating a false upside that unwinds"
  ],
  "watch_items": [
    "Official company press release or earnings call comment",
    "Regulatory filings (8-K, 10-Q updates) confirming the announcement",
    "Third-party verification from industry analysts or news"
  ],
  "source_quality_score": 15,
  "bullish_score": 30,
  "bearish_score": 70,
  "confidence_score": 20,
  "requires_manual_review": true,
  "manual_review_reason": "This is social-media-only hype without official company confirmation. Cannot generate a Buy recommendation from this alone. Wait for official announcement before reconsidering."
}
```

---

**Schema Version**: 1.0  
**Last Updated**: 2026-06-04
