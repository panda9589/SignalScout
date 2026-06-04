import axios, { AxiosInstance } from 'axios';

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
  async pasteDocument(payload: {
    rawText: string;
    companyId: string;
    sourceType: string;
  }) {
    const response = await this.client.post('/documents/manual', payload);
    return response.data;
  }

  // Company endpoints
  async getCompanyDetail(ticker: string) {
    const response = await this.client.get(`/companies/${ticker}`);
    return response.data;
  }

  async getWatchlistSummary() {
    const response = await this.client.get('/companies/watchlist/summary');
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
