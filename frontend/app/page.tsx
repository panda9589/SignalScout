'use client';

import Link from 'next/link';

const features = [
  {
    title: 'Manual document paste',
    description: 'Paste transcripts, articles, or research notes and extract structured events.',
  },
  {
    title: 'AI extraction',
    description: 'Pull out bull cases, bear cases, risks, watch items, and confidence scores.',
  },
  {
    title: 'Rule-based scoring',
    description: 'Convert extracted signals into Buy, Watch, Hold, or Avoid recommendations.',
  },
];

export default function Home() {
  return (
    <main className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-6 py-10">
        <header className="flex items-center justify-between border-b border-gray-200 pb-6">
          <div>
            <h1 className="text-3xl font-bold text-gray-950">SignalScout</h1>
            <p className="text-gray-600 mt-1">Market research radar for investment memos.</p>
          </div>
          <Link
            href="/watchlist"
            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded"
          >
            Open Watchlist
          </Link>
        </header>

        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6 py-8">
          <div className="lg:col-span-2 bg-white border border-gray-200 rounded-lg p-6">
            <h2 className="text-xl font-bold text-gray-950 mb-3">Phase 1 Workflow</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {features.map((feature) => (
                <div key={feature.title} className="border border-gray-200 rounded p-4">
                  <h3 className="font-semibold text-gray-950">{feature.title}</h3>
                  <p className="text-sm text-gray-600 mt-2">{feature.description}</p>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white border border-gray-200 rounded-lg p-6">
            <h2 className="text-xl font-bold text-gray-950 mb-3">Current Scope</h2>
            <ul className="space-y-2 text-sm text-gray-700">
              <li>Manual source ingestion</li>
              <li>Company watchlist</li>
              <li>Event extraction history</li>
              <li>Simple scoring and recommendations</li>
              <li>Stored document search and job history</li>
            </ul>
            <Link
              href="/research"
              className="mt-5 inline-flex bg-gray-900 hover:bg-gray-800 text-white font-semibold py-2 px-4 rounded"
            >
              Open Research
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
}
