'use client';

import { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { apiClient } from '@/lib/api-client';
import { WatchlistSummaryDto } from '@/lib/types';
import WatchlistGrid from '@/components/WatchlistGrid';
import { AiOutlineLoading3Quarters } from 'react-icons/ai';

export default function WatchlistPage() {
  const [companies, setCompanies] = useState<WatchlistSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchWatchlist();
  }, []);

  const fetchWatchlist = async () => {
    setLoading(true);
    try {
      const data = await apiClient.getWatchlistSummary();
      setCompanies(data);
    } catch (error: any) {
      toast.error('Failed to load watchlist');
    } finally {
      setLoading(false);
    }
  };

  const initializeData = async () => {
    try {
      toast.loading('Seeding initial data...');
      await apiClient.seedCompanies();
      await apiClient.seedThemes();
      await apiClient.seedSources();
      await fetchWatchlist();
      toast.dismiss();
      toast.success('Data initialized successfully!');
    } catch (error: any) {
      toast.dismiss();
      toast.error('Failed to initialize data');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-screen">
        <AiOutlineLoading3Quarters className="animate-spin text-4xl text-blue-600" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
      <div className="max-w-7xl mx-auto p-6">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">Investment Watchlist</h1>
          <p className="text-gray-600">
            Track companies and extract investment events to generate buy/sell/hold recommendations
          </p>
        </div>

        {/* Initialize Data Button */}
        {companies.length === 0 && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-8">
            <h2 className="font-bold text-blue-900 mb-2">Welcome to SignalScout!</h2>
            <p className="text-blue-700 mb-4">
              Click below to initialize the database with 5 sample companies (MRVL, AVGO, NVDA, MSFT, AMD)
            </p>
            <button
              onClick={initializeData}
              className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
            >
              Initialize Sample Data
            </button>
          </div>
        )}

        {/* Watchlist Grid */}
        <div className="bg-white rounded-lg shadow p-6">
          {companies.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-gray-600 text-lg">No companies in watchlist</p>
              <p className="text-gray-500">Initialize sample data to get started</p>
            </div>
          ) : (
            <WatchlistGrid companies={companies} />
          )}
        </div>
      </div>
    </div>
  );
}
