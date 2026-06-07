'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import toast from 'react-hot-toast';
import { AiOutlineLoading3Quarters, AiOutlineSearch } from 'react-icons/ai';
import { apiClient } from '@/lib/api-client';
import {
  DocumentSearchResultDto,
  EmbeddingJobResponse,
  JobRunDto,
  RssIngestionResponse,
  SecIngestionResponse,
} from '@/lib/types';

export default function ResearchPage() {
  const [query, setQuery] = useState('AI');
  const [results, setResults] = useState<DocumentSearchResultDto[]>([]);
  const [jobs, setJobs] = useState<JobRunDto[]>([]);
  const [selectedJob, setSelectedJob] = useState<JobRunDto | null>(null);
  const [ingestTicker, setIngestTicker] = useState('NVDA');
  const [reprocessDocumentId, setReprocessDocumentId] = useState('');
  const [rssUrl, setRssUrl] = useState('');
  const [rssTicker, setRssTicker] = useState('');
  const [searchMode, setSearchMode] = useState<'keyword' | 'semantic'>('keyword');
  const [lastIngestion, setLastIngestion] = useState<SecIngestionResponse | null>(null);
  const [lastRssIngestion, setLastRssIngestion] = useState<RssIngestionResponse | null>(null);
  const [lastEmbeddingJob, setLastEmbeddingJob] = useState<EmbeddingJobResponse | null>(null);
  const [searching, setSearching] = useState(false);
  const [ingesting, setIngesting] = useState(false);
  const [rssIngesting, setRssIngesting] = useState(false);
  const [reprocessing, setReprocessing] = useState(false);
  const [embedding, setEmbedding] = useState(false);
  const [loadingJobs, setLoadingJobs] = useState(true);

  const loadJobs = useCallback(async () => {
    setLoadingJobs(true);
    try {
      const data = await apiClient.getRecentJobs(10);
      setJobs(data);
    } catch (error) {
      toast.error('Failed to load job history');
    } finally {
      setLoadingJobs(false);
    }
  }, []);

  const runSearch = useCallback(async (searchText: string) => {
    const trimmed = searchText.trim();
    if (!trimmed) {
      setResults([]);
      return;
    }

    setSearching(true);
    try {
      const data = searchMode === 'semantic'
        ? await apiClient.semanticSearchDocuments(trimmed, 10)
        : await apiClient.searchDocuments(trimmed, 10);
      setResults(data);
    } catch (error) {
      toast.error(searchMode === 'semantic' ? 'Semantic search failed' : 'Failed to search documents');
    } finally {
      setSearching(false);
    }
  }, [searchMode]);

  useEffect(() => {
    void loadJobs();
    void runSearch('AI');
  }, [loadJobs, runSearch]);

  const submitSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void runSearch(query);
  };

  const runSecIngestion = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const ticker = ingestTicker.trim().toUpperCase();
    setIngesting(true);
    try {
      const response = await apiClient.runSecIngestion(ticker, 1);
      setLastIngestion(response);
      toast.success(`SEC ingest stored ${response.documentsStored} document(s)`);
      await loadJobs();
      if (ticker) {
        setQuery(ticker);
        await runSearch(ticker);
      }
    } catch (error) {
      toast.error('SEC ingestion failed');
    } finally {
      setIngesting(false);
    }
  };

  const runEmbeddings = async () => {
    setEmbedding(true);
    try {
      const response = await apiClient.runEmbeddings(100);
      setLastEmbeddingJob(response);
      toast.success(`Created ${response.embeddingsCreated} embedding(s)`);
      await loadJobs();
    } catch (error) {
      toast.error('Embedding job failed. Check OPENAI_API_KEY.');
    } finally {
      setEmbedding(false);
    }
  };

  const runRssIngestion = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const feedUrl = rssUrl.trim();
    if (!feedUrl) {
      toast.error('RSS feed URL is required');
      return;
    }

    setRssIngesting(true);
    try {
      const ticker = rssTicker.trim().toUpperCase();
      const response = await apiClient.runRssIngestion(feedUrl, ticker, 5);
      setLastRssIngestion(response);
      toast.success(`RSS ingest stored ${response.documentsStored} document(s)`);
      await loadJobs();
      if (ticker) {
        setQuery(ticker);
        await runSearch(ticker);
      }
    } catch (error) {
      toast.error('RSS ingestion failed');
    } finally {
      setRssIngesting(false);
    }
  };

  const loadJobDetail = async (jobId: number) => {
    try {
      const job = await apiClient.getJob(jobId);
      setSelectedJob(job);
    } catch (error) {
      toast.error('Failed to load job detail');
    }
  };

  const reprocessSecDocument = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const documentId = Number(reprocessDocumentId);
    if (!Number.isInteger(documentId) || documentId <= 0) {
      toast.error('Enter a valid document id');
      return;
    }

    setReprocessing(true);
    try {
      const response = await apiClient.reprocessSecDocument(documentId);
      toast.success(`Reprocessed document ${documentId}; recreated ${response.chunksCreated} chunks`);
      setLastIngestion(response);
      await loadJobs();
      await runSearch(query);
    } catch (error) {
      toast.error('SEC document reprocess failed');
    } finally {
      setReprocessing(false);
    }
  };

  return (
    <main className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-6 py-8">
        <header className="flex items-start justify-between gap-4 border-b border-gray-200 pb-6 mb-8">
          <div>
            <h1 className="text-3xl font-bold text-gray-950">Research Radar</h1>
            <p className="text-gray-600 mt-1">Search stored document chunks and monitor ingestion jobs.</p>
          </div>
          <Link
            href="/watchlist"
            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded"
          >
            Watchlist
          </Link>
        </header>

        <section className="grid grid-cols-1 xl:grid-cols-3 gap-6">
          <div className="xl:col-span-2 bg-white border border-gray-200 rounded-lg p-6">
            <form onSubmit={submitSearch} className="flex gap-3 mb-6">
              <label className="sr-only" htmlFor="document-search">
                Search documents
              </label>
              <input
                id="document-search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                className="flex-1 border border-gray-300 rounded px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Search stored documents"
              />
              <button
                type="submit"
                disabled={searching}
                className="inline-flex items-center gap-2 bg-gray-900 hover:bg-gray-800 disabled:bg-gray-500 text-white font-semibold py-2 px-4 rounded"
              >
                {searching ? (
                  <AiOutlineLoading3Quarters className="animate-spin" />
                ) : (
                  <AiOutlineSearch />
                )}
                Search
              </button>
            </form>

            <div className="flex items-center gap-2 mb-6">
              <button
                type="button"
                onClick={() => setSearchMode('keyword')}
                className={`px-3 py-1.5 rounded text-sm font-semibold ${
                  searchMode === 'keyword'
                    ? 'bg-gray-900 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                Keyword
              </button>
              <button
                type="button"
                onClick={() => setSearchMode('semantic')}
                className={`px-3 py-1.5 rounded text-sm font-semibold ${
                  searchMode === 'semantic'
                    ? 'bg-gray-900 text-white'
                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                }`}
              >
                Semantic
              </button>
            </div>

            <div className="space-y-4">
              {results.length === 0 && !searching ? (
                <div className="border border-dashed border-gray-300 rounded p-8 text-center text-gray-600">
                  No matching document chunks.
                </div>
              ) : (
                results.map((result) => (
                  <article key={result.chunkId} className="border border-gray-200 rounded p-4">
                    <div className="flex items-center justify-between gap-3 mb-2">
                      <div className="font-semibold text-gray-950">
                        {result.ticker || 'Unknown'} - Document {result.documentId}
                      </div>
                      <span className="text-xs uppercase tracking-wide text-gray-500">
                        {result.sourceType} - Chunk {result.chunkIndex}
                        {typeof result.distance === 'number' ? ` - ${result.distance.toFixed(4)}` : ''}
                      </span>
                    </div>
                    {result.title && <p className="text-sm text-gray-500 mb-2">{result.title}</p>}
                    <p className="text-sm leading-6 text-gray-700">{result.snippet}</p>
                  </article>
                ))
              )}
            </div>
          </div>

          <aside className="bg-white border border-gray-200 rounded-lg p-6">
            <div className="border-b border-gray-200 pb-5 mb-5">
              <h2 className="text-xl font-bold text-gray-950 mb-4">SEC Ingestion</h2>
              <form onSubmit={runSecIngestion} className="flex gap-3">
                <label className="sr-only" htmlFor="sec-ticker">
                  Ticker
                </label>
                <input
                  id="sec-ticker"
                  value={ingestTicker}
                  onChange={(event) => setIngestTicker(event.target.value.toUpperCase())}
                  className="w-28 border border-gray-300 rounded px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="NVDA"
                  maxLength={10}
                />
                <button
                  type="submit"
                  disabled={ingesting}
                  className="inline-flex flex-1 items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-2 px-4 rounded"
                >
                  {ingesting && <AiOutlineLoading3Quarters className="animate-spin" />}
                  Run SEC
                </button>
              </form>
              {lastIngestion && (
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <div className="border border-gray-200 rounded p-3">
                    <div className="text-gray-500">Stored</div>
                    <div className="text-lg font-bold text-gray-950">{lastIngestion.documentsStored}</div>
                  </div>
                  <div className="border border-gray-200 rounded p-3">
                    <div className="text-gray-500">Chunks</div>
                    <div className="text-lg font-bold text-gray-950">{lastIngestion.chunksCreated}</div>
                  </div>
                  <div className="border border-gray-200 rounded p-3">
                    <div className="text-gray-500">Skipped</div>
                    <div className="text-lg font-bold text-gray-950">{lastIngestion.duplicatesSkipped}</div>
                  </div>
                  <div className="border border-gray-200 rounded p-3">
                    <div className="text-gray-500">Job</div>
                    <div className="text-lg font-bold text-gray-950">#{lastIngestion.jobRunId}</div>
                  </div>
                </div>
              )}
              <form onSubmit={reprocessSecDocument} className="mt-5 border-t border-gray-200 pt-5">
                <h3 className="font-bold text-gray-950 mb-3">SEC Cleanup</h3>
                <div className="flex gap-3">
                  <label className="sr-only" htmlFor="sec-document-id">
                    SEC document id
                  </label>
                  <input
                    id="sec-document-id"
                    value={reprocessDocumentId}
                    onChange={(event) => setReprocessDocumentId(event.target.value)}
                    className="w-32 border border-gray-300 rounded px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Doc id"
                    inputMode="numeric"
                  />
                  <button
                    type="submit"
                    disabled={reprocessing}
                    className="inline-flex flex-1 items-center justify-center gap-2 bg-gray-700 hover:bg-gray-800 disabled:bg-gray-500 text-white font-semibold py-2 px-4 rounded"
                  >
                    {reprocessing && <AiOutlineLoading3Quarters className="animate-spin" />}
                    Reprocess
                  </button>
                </div>
                <p className="text-xs text-gray-500 mt-2">
                  Recreates chunks for a SEC filing. Run embeddings again after cleanup.
                </p>
              </form>
              <form onSubmit={runRssIngestion} className="mt-5 border-t border-gray-200 pt-5">
                <h3 className="font-bold text-gray-950 mb-3">RSS Ingestion</h3>
                <label className="sr-only" htmlFor="rss-url">
                  RSS feed URL
                </label>
                <input
                  id="rss-url"
                  value={rssUrl}
                  onChange={(event) => setRssUrl(event.target.value)}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="https://example.com/feed.xml"
                />
                <div className="flex gap-3 mt-3">
                  <label className="sr-only" htmlFor="rss-ticker">
                    Optional ticker
                  </label>
                  <input
                    id="rss-ticker"
                    value={rssTicker}
                    onChange={(event) => setRssTicker(event.target.value.toUpperCase())}
                    className="w-28 border border-gray-300 rounded px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Ticker"
                    maxLength={10}
                  />
                  <button
                    type="submit"
                    disabled={rssIngesting}
                    className="inline-flex flex-1 items-center justify-center gap-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-2 px-4 rounded"
                  >
                    {rssIngesting && <AiOutlineLoading3Quarters className="animate-spin" />}
                    Run RSS
                  </button>
                </div>
                {lastRssIngestion && (
                  <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                    <div className="border border-gray-200 rounded p-3">
                      <div className="text-gray-500">Stored</div>
                      <div className="text-lg font-bold text-gray-950">{lastRssIngestion.documentsStored}</div>
                    </div>
                    <div className="border border-gray-200 rounded p-3">
                      <div className="text-gray-500">Skipped</div>
                      <div className="text-lg font-bold text-gray-950">{lastRssIngestion.duplicatesSkipped}</div>
                    </div>
                    <div className="border border-gray-200 rounded p-3">
                      <div className="text-gray-500">Matched</div>
                      <div className="text-lg font-bold text-gray-950">{lastRssIngestion.autoMatchedDocuments}</div>
                    </div>
                    <div className="border border-gray-200 rounded p-3">
                      <div className="text-gray-500">Chunks</div>
                      <div className="text-lg font-bold text-gray-950">{lastRssIngestion.chunksCreated}</div>
                    </div>
                  </div>
                )}
              </form>
              <button
                type="button"
                onClick={() => void runEmbeddings()}
                disabled={embedding}
                className="mt-4 inline-flex w-full items-center justify-center gap-2 bg-gray-900 hover:bg-gray-800 disabled:bg-gray-500 text-white font-semibold py-2 px-4 rounded"
              >
                {embedding && <AiOutlineLoading3Quarters className="animate-spin" />}
                Generate Embeddings
              </button>
              {lastEmbeddingJob && (
                <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
                  <div className="border border-gray-200 rounded p-3">
                    <div className="text-gray-500">Created</div>
                    <div className="text-lg font-bold text-gray-950">{lastEmbeddingJob.embeddingsCreated}</div>
                  </div>
                  <div className="border border-gray-200 rounded p-3">
                    <div className="text-gray-500">AI Calls</div>
                    <div className="text-lg font-bold text-gray-950">{lastEmbeddingJob.aiCalls}</div>
                  </div>
                </div>
              )}
            </div>

            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-gray-950">Job History</h2>
              <button
                type="button"
                onClick={() => void loadJobs()}
                className="text-sm font-semibold text-blue-700 hover:text-blue-900"
              >
                Refresh
              </button>
            </div>

            {loadingJobs ? (
              <div className="flex items-center gap-2 text-gray-600">
                <AiOutlineLoading3Quarters className="animate-spin" />
                Loading jobs
              </div>
            ) : jobs.length === 0 ? (
              <p className="text-sm text-gray-600">No jobs recorded yet.</p>
            ) : (
              <div className="space-y-3">
                {jobs.map((job) => (
                  <button
                    key={job.id}
                    type="button"
                    onClick={() => void loadJobDetail(job.id)}
                    className="w-full text-left border border-gray-200 rounded p-3 hover:border-blue-300 hover:bg-blue-50"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div className="font-semibold text-gray-950">{job.jobName}</div>
                      <span
                        className={`text-xs font-semibold uppercase ${
                          job.status === 'success' ? 'text-green-700' : 'text-red-700'
                        }`}
                      >
                        {job.status}
                      </span>
                    </div>
                    <div className="text-sm text-gray-600 mt-2">
                      {job.documentsProcessed}/{job.documentsFound} documents - {job.aiCalls} AI calls
                    </div>
                    <div className="text-xs text-gray-500 mt-1">{job.startedAt}</div>
                    {job.errorMessage && (
                      <p className="text-sm text-red-700 mt-2">{job.errorMessage}</p>
                    )}
                  </button>
                ))}
              </div>
            )}

            {selectedJob && (
              <div className="mt-5 border-t border-gray-200 pt-5">
                <div className="flex items-center justify-between gap-3 mb-3">
                  <h3 className="font-bold text-gray-950">Job #{selectedJob.id}</h3>
                  <button
                    type="button"
                    onClick={() => setSelectedJob(null)}
                    className="text-sm font-semibold text-gray-500 hover:text-gray-900"
                  >
                    Close
                  </button>
                </div>
                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div className="border border-gray-200 rounded p-3">
                    <dt className="text-gray-500">Status</dt>
                    <dd className="font-semibold text-gray-950">{selectedJob.status}</dd>
                  </div>
                  <div className="border border-gray-200 rounded p-3">
                    <dt className="text-gray-500">AI Calls</dt>
                    <dd className="font-semibold text-gray-950">{selectedJob.aiCalls}</dd>
                  </div>
                  <div className="border border-gray-200 rounded p-3">
                    <dt className="text-gray-500">Found</dt>
                    <dd className="font-semibold text-gray-950">{selectedJob.documentsFound}</dd>
                  </div>
                  <div className="border border-gray-200 rounded p-3">
                    <dt className="text-gray-500">Processed</dt>
                    <dd className="font-semibold text-gray-950">{selectedJob.documentsProcessed}</dd>
                  </div>
                </dl>
                <div className="text-xs text-gray-500 mt-3">
                  Started: {selectedJob.startedAt}
                  {selectedJob.finishedAt ? ` | Finished: ${selectedJob.finishedAt}` : ''}
                </div>
                {selectedJob.errorMessage && (
                  <p className="text-sm text-red-700 mt-3">{selectedJob.errorMessage}</p>
                )}
              </div>
            )}
          </aside>
        </section>
      </div>
    </main>
  );
}
