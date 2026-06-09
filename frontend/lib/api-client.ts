import axios, { AxiosInstance } from 'axios';
import {
  DocumentExtractedResponse,
  DocumentSearchResultDto,
  EmbeddingJobResponse,
  InvestmentThesisDto,
  JobRunDto,
  ManualDocumentPasteRequest,
  PortfolioAccountDto,
  PortfolioActionReportResponse,
  PortfolioHoldingDto,
  ReportDetailDto,
  ReportSummaryDto,
  RssIngestionResponse,
  RiskSettingsDto,
  SecIngestionResponse,
  UpcomingEventDto,
  UpsertPortfolioHoldingRequest,
  WatchlistSummaryDto,
} from './types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });
  }

  // Document endpoints
  async pasteDocument(payload: ManualDocumentPasteRequest): Promise<DocumentExtractedResponse> {
    const response = await this.client.post('/documents/manual', payload);
    return response.data;
  }

  async searchDocuments(query: string, limit = 10): Promise<DocumentSearchResultDto[]> {
    const response = await this.client.get('/documents/search', {
      params: { q: query, limit },
    });
    return response.data;
  }

  async semanticSearchDocuments(query: string, limit = 10): Promise<DocumentSearchResultDto[]> {
    const response = await this.client.get('/documents/semantic-search', {
      params: { q: query, limit },
    });
    return response.data;
  }

  // Company endpoints
  async getCompanyDetail(ticker: string) {
    const response = await this.client.get(`/companies/${ticker}`);
    return response.data;
  }

  async getWatchlistSummary(): Promise<WatchlistSummaryDto[]> {
    const response = await this.client.get('/companies/watchlist/summary');
    return response.data;
  }

  // Job endpoints
  async getRecentJobs(limit = 20): Promise<JobRunDto[]> {
    const response = await this.client.get('/jobs', {
      params: { limit },
    });
    return response.data;
  }

  async getJob(id: number): Promise<JobRunDto> {
    const response = await this.client.get(`/jobs/${id}`);
    return response.data;
  }

  // Ingestion endpoints
  async runSecIngestion(ticker?: string, limit = 1): Promise<SecIngestionResponse> {
    const response = await this.client.post('/ingestion/sec/run', null, {
      params: { ticker: ticker || undefined, limit },
    });
    return response.data;
  }

  async reprocessSecDocument(documentId: number): Promise<SecIngestionResponse> {
    const response = await this.client.post(`/ingestion/sec/documents/${documentId}/reprocess`);
    return response.data;
  }

  async runRssIngestion(feedUrl: string, ticker?: string, limit = 5): Promise<RssIngestionResponse> {
    const response = await this.client.post('/ingestion/rss/run', null, {
      params: { feedUrl, ticker: ticker || undefined, limit },
    });
    return response.data;
  }

  async runEmbeddings(limit = 100): Promise<EmbeddingJobResponse> {
    const response = await this.client.post('/embeddings/run', null, {
      params: { limit },
    });
    return response.data;
  }

  // Portfolio endpoints
  async getPortfolioAccounts(): Promise<PortfolioAccountDto[]> {
    const response = await this.client.get('/portfolio/accounts');
    return response.data;
  }

  async createPortfolioAccount(payload: {
    accountName: string;
    accountType: string;
    baseCurrency: string;
  }): Promise<PortfolioAccountDto> {
    const response = await this.client.post('/portfolio/accounts', payload);
    return response.data;
  }

  async updatePortfolioAccount(id: number, payload: {
    accountName: string;
    accountType: string;
    baseCurrency: string;
  }): Promise<PortfolioAccountDto> {
    const response = await this.client.put(`/portfolio/accounts/${id}`, payload);
    return response.data;
  }

  async deletePortfolioAccount(id: number): Promise<void> {
    await this.client.delete(`/portfolio/accounts/${id}`);
  }

  async getPortfolioHoldings(): Promise<PortfolioHoldingDto[]> {
    const response = await this.client.get('/portfolio/holdings');
    return response.data;
  }

  async createPortfolioHolding(payload: UpsertPortfolioHoldingRequest): Promise<PortfolioHoldingDto> {
    const response = await this.client.post('/portfolio/holdings', payload);
    return response.data;
  }

  async updatePortfolioHolding(id: number, payload: UpsertPortfolioHoldingRequest): Promise<PortfolioHoldingDto> {
    const response = await this.client.put(`/portfolio/holdings/${id}`, payload);
    return response.data;
  }

  async deletePortfolioHolding(id: number): Promise<void> {
    await this.client.delete(`/portfolio/holdings/${id}`);
  }

  async getRiskSettings(): Promise<RiskSettingsDto> {
    const response = await this.client.get('/portfolio/risk-settings');
    return response.data;
  }

  async updateRiskSettings(payload: RiskSettingsDto): Promise<RiskSettingsDto> {
    const response = await this.client.put('/portfolio/risk-settings', payload);
    return response.data;
  }

  async getTheses(): Promise<InvestmentThesisDto[]> {
    const response = await this.client.get('/theses');
    return response.data;
  }

  async createThesis(payload: {
    companyId: number;
    thesisText: string;
    buyReason?: string;
    expectedTimeHorizonMonths?: number;
    status?: string;
  }): Promise<InvestmentThesisDto> {
    const response = await this.client.post('/theses', payload);
    return response.data;
  }

  async updateThesis(id: number, payload: {
    companyId: number;
    thesisText: string;
    buyReason?: string;
    expectedTimeHorizonMonths?: number;
    status?: string;
  }): Promise<InvestmentThesisDto> {
    const response = await this.client.put(`/theses/${id}`, payload);
    return response.data;
  }

  async deleteThesis(id: number): Promise<void> {
    await this.client.delete(`/theses/${id}`);
  }

  async generatePortfolioActionReport(): Promise<PortfolioActionReportResponse> {
    const response = await this.client.post('/recommendations/generate');
    return response.data;
  }

  // Report endpoints
  async getReports(reportType?: string, limit = 20): Promise<ReportSummaryDto[]> {
    const response = await this.client.get('/reports', {
      params: { reportType: reportType || undefined, limit },
    });
    return response.data;
  }

  async getReport(id: number): Promise<ReportDetailDto> {
    const response = await this.client.get(`/reports/${id}`);
    return response.data;
  }

  async generateReport(reportType: 'daily' | 'weekly' | 'action'): Promise<ReportDetailDto> {
    const response = await this.client.post('/reports/generate', null, {
      params: { reportType },
    });
    return response.data;
  }

  async getUpcomingEvents(limit = 20): Promise<UpcomingEventDto[]> {
    const response = await this.client.get('/reports/upcoming-events', {
      params: { limit },
    });
    return response.data;
  }

  async emailReport(id: number): Promise<void> {
    await this.client.post(`/reports/${id}/email`);
  }

  // Admin endpoints
  async seedCompanies() {
    const response = await this.client.post('/admin/seed-companies');
    return response.data;
  }

  async seedThemes() {
    const response = await this.client.post('/admin/seed-themes');
    return response.data;
  }

  async seedSources() {
    const response = await this.client.post('/admin/seed-sources');
    return response.data;
  }
}

export const apiClient = new ApiClient();
