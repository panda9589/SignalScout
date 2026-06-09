'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import toast from 'react-hot-toast';
import { AiOutlineLoading3Quarters } from 'react-icons/ai';
import { apiClient } from '@/lib/api-client';
import { ReportDetailDto, ReportSummaryDto, UpcomingEventDto } from '@/lib/types';

type ReportType = 'daily' | 'weekly' | 'action';

export default function ReportsPage() {
  const [reports, setReports] = useState<ReportSummaryDto[]>([]);
  const [selectedReport, setSelectedReport] = useState<ReportDetailDto | null>(null);
  const [upcomingEvents, setUpcomingEvents] = useState<UpcomingEventDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState<ReportType | null>(null);
  const [emailing, setEmailing] = useState(false);

  const loadReports = useCallback(async () => {
    setLoading(true);
    try {
      const [reportData, eventData] = await Promise.all([
        apiClient.getReports(undefined, 20),
        apiClient.getUpcomingEvents(10),
      ]);
      setReports(reportData);
      setUpcomingEvents(eventData);
      if (!selectedReport && reportData.length > 0) {
        const detail = await apiClient.getReport(reportData[0].id);
        setSelectedReport(detail);
      }
    } catch (error) {
      toast.error('Failed to load reports');
    } finally {
      setLoading(false);
    }
  }, [selectedReport]);

  useEffect(() => {
    void loadReports();
  }, [loadReports]);

  const generateReport = async (reportType: ReportType) => {
    setGenerating(reportType);
    try {
      const report = await apiClient.generateReport(reportType);
      setSelectedReport(report);
      toast.success(`${report.title} generated`);
      const reportData = await apiClient.getReports(undefined, 20);
      setReports(reportData);
    } catch (error) {
      toast.error('Failed to generate report');
    } finally {
      setGenerating(null);
    }
  };

  const openReport = async (reportId: number) => {
    try {
      const report = await apiClient.getReport(reportId);
      setSelectedReport(report);
    } catch (error) {
      toast.error('Failed to open report');
    }
  };

  const pdfUrl = useMemo(() => {
    if (!selectedReport) {
      return '';
    }
    return `${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api'}/reports/${selectedReport.id}/pdf`;
  }, [selectedReport]);

  const emailSelectedReport = async () => {
    if (!selectedReport) {
      return;
    }
    setEmailing(true);
    try {
      await apiClient.emailReport(selectedReport.id);
      toast.success('Report PDF emailed');
    } catch (error) {
      toast.error('Failed to email report PDF');
    } finally {
      setEmailing(false);
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <AiOutlineLoading3Quarters className="animate-spin text-4xl text-blue-600" />
      </div>
    );
  }

  return (
    <main className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-7xl px-6 py-8">
        <header className="mb-8 flex items-start justify-between gap-4 border-b border-gray-200 pb-6">
          <div>
            <Link href="/" className="text-3xl font-bold text-gray-950 hover:text-blue-700">
              SignalScout
            </Link>
            <h1 className="mt-2 text-xl font-bold text-gray-950">Reports</h1>
          </div>
          <nav className="flex flex-wrap gap-2">
            <Link href="/watchlist" className="rounded bg-white px-4 py-2 font-semibold text-gray-800 ring-1 ring-gray-200 hover:bg-gray-100">
              Watchlist
            </Link>
            <Link href="/research" className="rounded bg-white px-4 py-2 font-semibold text-gray-800 ring-1 ring-gray-200 hover:bg-gray-100">
              Research
            </Link>
            <Link href="/portfolio" className="rounded bg-white px-4 py-2 font-semibold text-gray-800 ring-1 ring-gray-200 hover:bg-gray-100">
              Portfolio
            </Link>
          </nav>
        </header>

        <section className="mb-6 grid grid-cols-1 gap-3 md:grid-cols-3">
          {(['daily', 'weekly', 'action'] as ReportType[]).map((reportType) => (
            <button
              key={reportType}
              type="button"
              onClick={() => void generateReport(reportType)}
              disabled={generating !== null}
              className="inline-flex items-center justify-center gap-2 rounded bg-gray-900 px-4 py-3 font-semibold capitalize text-white hover:bg-gray-800 disabled:bg-gray-500"
            >
              {generating === reportType && <AiOutlineLoading3Quarters className="animate-spin" />}
              Generate {reportType}
            </button>
          ))}
        </section>

        <section className="grid grid-cols-1 gap-6 xl:grid-cols-3">
          <aside className="space-y-6">
            <div className="rounded border border-gray-200 bg-white p-6">
              <h2 className="mb-4 text-xl font-bold text-gray-950">Saved Reports</h2>
              {reports.length === 0 ? (
                <p className="text-sm text-gray-600">No reports generated yet.</p>
              ) : (
                <div className="space-y-3">
                  {reports.map((report) => (
                    <button
                      key={report.id}
                      type="button"
                      onClick={() => void openReport(report.id)}
                      className={`w-full rounded border p-3 text-left hover:border-blue-300 hover:bg-blue-50 ${
                        selectedReport?.id === report.id ? 'border-blue-300 bg-blue-50' : 'border-gray-200'
                      }`}
                    >
                      <div className="font-semibold text-gray-950">{report.title}</div>
                      <div className="mt-1 text-xs uppercase text-gray-500">
                        {report.reportType} - {report.createdAt}
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="rounded border border-gray-200 bg-white p-6">
              <h2 className="mb-4 text-xl font-bold text-gray-950">Upcoming Events</h2>
              {upcomingEvents.length === 0 ? (
                <p className="text-sm text-gray-600">No workflow items detected.</p>
              ) : (
                <div className="space-y-3">
                  {upcomingEvents.map((event, index) => (
                    <article key={`${event.eventType}-${event.ticker || index}`} className="rounded border border-gray-200 p-3">
                      <div className="flex items-center justify-between gap-3">
                        <div className="font-semibold text-gray-950">{event.ticker || 'Portfolio'}</div>
                        <span className="text-xs font-semibold uppercase text-gray-500">{event.priority}</span>
                      </div>
                      <div className="mt-1 text-sm text-gray-600">{event.dueDate} - {event.title}</div>
                      <p className="mt-2 text-sm leading-6 text-gray-700">{event.reason}</p>
                    </article>
                  ))}
                </div>
              )}
            </div>
          </aside>

          <div className="xl:col-span-2">
            <div className="rounded border border-gray-200 bg-white p-6">
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <h2 className="text-xl font-bold text-gray-950">
                  {selectedReport ? selectedReport.title : 'Report Preview'}
                </h2>
                {selectedReport && (
                  <div className="flex flex-wrap gap-2">
                    <a
                      href={pdfUrl}
                      className="rounded bg-white px-4 py-2 font-semibold text-gray-800 ring-1 ring-gray-200 hover:bg-gray-100"
                    >
                      Download PDF
                    </a>
                    <button
                      type="button"
                      onClick={() => void emailSelectedReport()}
                      disabled={emailing}
                      className="inline-flex items-center gap-2 rounded bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 disabled:bg-blue-400"
                    >
                      {emailing && <AiOutlineLoading3Quarters className="animate-spin" />}
                      Email PDF
                    </button>
                  </div>
                )}
              </div>
              {!selectedReport ? (
                <div className="rounded border border-dashed border-gray-300 p-8 text-center text-gray-600">
                  Generate or select a report.
                </div>
              ) : (
                <pre className="whitespace-pre-wrap rounded bg-gray-950 p-5 text-sm leading-6 text-gray-100">
                  {selectedReport.reportMarkdown}
                </pre>
              )}
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
