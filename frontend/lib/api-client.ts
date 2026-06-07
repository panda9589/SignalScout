import axios, { AxiosInstance } from 'axios';
import {
  DocumentExtractedResponse,
  DocumentSearchResultDto,
  EmbeddingJobResponse,
  JobRunDto,
  ManualDocumentPasteRequest,
  RssIngestionResponse,
  SecIngestionResponse,
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
