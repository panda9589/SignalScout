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
