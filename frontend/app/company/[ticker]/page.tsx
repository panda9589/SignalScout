'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import toast from 'react-hot-toast';
import { apiClient } from '@/lib/api-client';
import { CompanyDetailResponse, DocumentExtractedResponse } from '@/lib/types';
import EventCard from '@/components/EventCard';
import ManualPasteForm from '@/components/ManualPasteForm';
import ExtractionResult from '@/components/ExtractionResult';
import { AiOutlineLoading3Quarters } from 'react-icons/ai';

interface PageProps {
  params: {
    ticker: string;
  };
}

export default function CompanyPage({ params }: PageProps) {
  const [data, setData] = useState<CompanyDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [result, setResult] = useState<DocumentExtractedResponse | null>(null);

  useEffect(() => {
    fetchCompanyDetail();
  }, [params.ticker]);

  const fetchCompanyDetail = async () => {
    setLoading(true);
    try {
      const response = await apiClient.getCompanyDetail(params.ticker.toUpperCase());
      setData(response);
    } catch (error: any) {
      const message =
        error.response?.data?.message || error.message || 'Failed to load company';
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  const handleExtractionSuccess = (extractedResult: DocumentExtractedResponse) => {
    setResult(extractedResult);
    // Refresh company data
    fetchCompanyDetail();
  };

  const recommendationColor = (rec?: string) => {
    switch (rec) {
      case 'BUY':
        return 'bg-green-100 border-green-300 text-green-900';
      case 'WATCH':
        return 'bg-blue-100 border-blue-300 text-blue-900';
      case 'HOLD':
        return 'bg-gray-100 border-gray-300 text-gray-900';
      case 'AVOID':
        return 'bg-red-100 border-red-300 text-red-900';
      default:
        return 'bg-gray-100 border-gray-300 text-gray-900';
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <AiOutlineLoading3Quarters className="animate-spin text-4xl text-blue-600" />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="text-center py-12">
        <h2 className="text-2xl font-bold text-gray-900 mb-4">Company Not Found</h2>
        <Link href="/watchlist" className="text-blue-600 hover:text-blue-700 font-semibold">
          Back to Watchlist
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      <div className="max-w-6xl mx-auto p-6">
        {/* Header */}
        <div className="mb-8">
          <Link href="/watchlist" className="text-blue-600 hover:text-blue-700 mb-4 inline-block">
            ← Back to Watchlist
          </Link>

          <div className="bg-white rounded-lg shadow p-6 mb-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h1 className="text-4xl font-bold text-gray-900">{data.company.ticker}</h1>
                <p className="text-xl text-gray-600">{data.company.name}</p>
              </div>
              {data.latestRecommendation && (
                <div
                  className={`px-4 py-2 rounded-lg border text-lg font-bold ${recommendationColor(
                    data.latestRecommendation
                  )}`}
                >
                  {data.latestRecommendation}
                </div>
              )}
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <p className="text-sm text-gray-600">Sector</p>
                <p className="font-semibold">{data.company.sector || 'N/A'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-600">Industry</p>
                <p className="font-semibold">{data.company.industry || 'N/A'}</p>
              </div>
              {data.latestStockScore !== undefined && (
                <div>
                  <p className="text-sm text-gray-600">Latest Score</p>
                  <p className="text-2xl font-bold text-blue-600">{data.latestStockScore}/100</p>
                </div>
              )}
              <div>
                <p className="text-sm text-gray-600">Exchange</p>
                <p className="font-semibold">{data.company.exchange || 'N/A'}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column: Recent Events */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-2xl font-bold text-gray-900 mb-4">
                Recent Events ({data.recentEvents.length})
              </h2>

              {data.recentEvents.length === 0 ? (
                <p className="text-gray-600">No events extracted yet. Paste a document below to get started.</p>
              ) : (
                <div className="space-y-4">
                  {data.recentEvents.map((event) => (
                    <EventCard key={event.id} event={event} />
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Right Column: Manual Paste Form */}
          <div>
            <div className="bg-white rounded-lg shadow p-6 sticky top-6">
              <h3 className="text-xl font-bold text-gray-900 mb-4">Extract New Event</h3>
              <ManualPasteForm
                companyId={data.company.id}
                companyTicker={data.company.ticker}
                onSuccess={handleExtractionSuccess}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Extraction Result Modal */}
      {result && (
        <ExtractionResult result={result} onClose={() => setResult(null)} />
      )}
    </div>
  );
}
