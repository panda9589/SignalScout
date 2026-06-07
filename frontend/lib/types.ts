/**
 * Type definitions for SignalScout API responses
 */

export interface Company {
  id: number;
  ticker: string;
  name: string;
  sector: string;
  industry?: string;
  cik?: string;
  exchange?: string;
  country?: string;
  currency?: string;
}

export interface Evidence {
  text: string;
  location?: string;
  evidenceType?: string;
}

export interface ExtractedEvent {
  id: number;
  eventType: string;
  eventDate?: string;
  summary: string;
  whatChanged?: string;
  bullishScore: number;
  bearishScore: number;
  sourceQualityScore: number;
  confidenceScore: number;
  bullCase?: string[];
  bearCase?: string[];
  risks?: string[];
  watchItems?: string[];
  requiresManualReview?: boolean;
  manualReviewReason?: string;
  createdAt: string;
}

export interface CompanyDetailResponse {
  company: Company;
  latestStockScore?: number;
  latestRecommendation?: string;
  recentEvents: ExtractedEvent[];
}

export interface WatchlistSummaryDto {
  companyId: number;
  ticker: string;
  name: string;
  sector: string;
  latestStockScore?: number;
  latestRecommendation?: string;
  recentEventCount: number;
}

export interface DocumentExtractedResponse {
  documentId: number;
  eventId: number;
  ticker: string;
  companyName: string;
  eventType: string;
  summary: string;
  whatChanged?: string;
  bullishScore: number;
  bearishScore: number;
  sourceQualityScore: number;
  confidenceScore: number;
  stockScore: number;
  recommendation: string;
  bullCase?: string[];
  bearCase?: string[];
  risks?: string[];
  watchItems?: string[];
  requiresManualReview?: boolean;
  manualReviewReason?: string;
  createdAt: string;
}

export interface ManualDocumentPasteRequest {
  rawText: string;
  companyId: string;
  sourceType: string;
}

export interface DocumentSearchResultDto {
  documentId: number;
  chunkId: number;
  chunkIndex: number;
  ticker?: string;
  title?: string;
  sourceType: string;
  snippet: string;
  distance?: number;
}

export interface JobRunDto {
  id: number;
  jobName: string;
  startedAt: string;
  finishedAt?: string;
  status: string;
  errorMessage?: string;
  documentsFound: number;
  documentsProcessed: number;
  aiCalls: number;
  estimatedCostUsd?: number;
}

export interface SecIngestionResponse {
  jobRunId: number;
  companiesScanned: number;
  filingsFound: number;
  documentsStored: number;
  duplicatesSkipped: number;
  chunksCreated: number;
}

export interface RssIngestionResponse {
  jobRunId: number;
  itemsFound: number;
  documentsStored: number;
  duplicatesSkipped: number;
  autoMatchedDocuments: number;
  chunksCreated: number;
}

export interface EmbeddingJobResponse {
  jobRunId: number;
  chunksFound: number;
  embeddingsCreated: number;
  aiCalls: number;
  modelName: string;
}

export interface PortfolioAccountDto {
  id: number;
  accountName: string;
  accountType: string;
  baseCurrency: string;
}

export interface UpsertPortfolioHoldingRequest {
  accountId: number;
  companyId?: number;
  symbol: string;
  quantity?: number;
  avgCost?: number;
  marketValueCad?: number;
}

export interface PortfolioHoldingDto {
  id: number;
  accountId: number;
  accountName: string;
  companyId?: number;
  ticker?: string;
  companyName?: string;
  symbol: string;
  quantity?: number;
  avgCost?: number;
  marketValueCad?: number;
  portfolioWeight?: number;
}

export interface InvestmentThesisDto {
  id: number;
  companyId: number;
  ticker: string;
  companyName: string;
  thesisText: string;
  buyReason?: string;
  expectedTimeHorizonMonths?: number;
  status: string;
}

export interface RiskSettingsDto {
  maxSingleStockPositionPct: number;
  maxSectorExposurePct: number;
  minScoreForNewBuy: number;
  minSourceQualityForNewBuy: number;
}

export interface PortfolioActionDto {
  companyId: number;
  ticker: string;
  companyName: string;
  sector?: string;
  action: string;
  stockScore?: number;
  sourceQualityScore?: number;
  latestRecommendation?: string;
  currentWeightPct: number;
  sectorExposurePct: number;
  hasHolding: boolean;
  hasActiveThesis: boolean;
  reason: string;
  buyTrigger: string;
  sellTrigger: string;
  riskNotes: string;
}

export interface PortfolioActionReportResponse {
  generatedAt: string;
  totalMarketValueCad: number;
  riskSettings: RiskSettingsDto;
  actions: PortfolioActionDto[];
}
