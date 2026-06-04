import { WatchlistSummaryDto } from '@/lib/types';
import Link from 'next/link';

interface WatchlistGridProps {
  companies: WatchlistSummaryDto[];
}

export default function WatchlistGrid({ companies }: WatchlistGridProps) {
  const recommendationColor = (rec?: string) => {
    switch (rec) {
      case 'BUY':
        return 'text-green-600 font-bold';
      case 'WATCH':
        return 'text-blue-600 font-bold';
      case 'HOLD':
        return 'text-gray-600';
      case 'AVOID':
        return 'text-red-600';
      default:
        return 'text-gray-400';
    }
  };

  const recommendationBg = (rec?: string) => {
    switch (rec) {
      case 'BUY':
        return 'bg-green-50 border-green-200';
      case 'WATCH':
        return 'bg-blue-50 border-blue-200';
      case 'HOLD':
        return 'bg-gray-50 border-gray-200';
      case 'AVOID':
        return 'bg-red-50 border-red-200';
      default:
        return 'bg-gray-50 border-gray-200';
    }
  };

  if (companies.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-600 mb-4">No companies in your watchlist yet</p>
        <Link
          href="/watchlist"
          className="text-blue-600 hover:text-blue-700 font-semibold"
        >
          Add companies to get started
        </Link>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {companies.map((company) => (
        <Link key={company.companyId} href={`/company/${company.ticker}`}>
          <div
            className={`border rounded-lg p-4 hover:shadow-lg transition cursor-pointer ${recommendationBg(
              company.latestRecommendation
            )}`}
          >
            <div className="flex justify-between items-start mb-3">
              <div>
                <h3 className="font-bold text-lg">{company.ticker}</h3>
                <p className="text-sm text-gray-600">{company.name}</p>
              </div>
              {company.latestRecommendation && (
                <span className={`text-sm font-bold ${recommendationColor(company.latestRecommendation)}`}>
                  {company.latestRecommendation}
                </span>
              )}
            </div>

            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-gray-600">Sector</span>
                <span className="font-semibold">{company.sector}</span>
              </div>

              {company.latestStockScore !== undefined && (
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Score</span>
                  <span className="font-semibold">{company.latestStockScore}/100</span>
                </div>
              )}

              <div className="flex justify-between text-sm pt-2 border-t">
                <span className="text-gray-600">Recent Events</span>
                <span className="font-semibold text-blue-600">{company.recentEventCount}</span>
              </div>
            </div>
          </div>
        </Link>
      ))}
    </div>
  );
}
