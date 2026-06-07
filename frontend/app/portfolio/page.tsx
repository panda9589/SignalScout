'use client';

import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import toast from 'react-hot-toast';
import { AiOutlineLoading3Quarters } from 'react-icons/ai';
import { apiClient } from '@/lib/api-client';
import {
  InvestmentThesisDto,
  PortfolioAccountDto,
  PortfolioActionReportResponse,
  PortfolioHoldingDto,
  RiskSettingsDto,
  WatchlistSummaryDto,
} from '@/lib/types';

const defaultRiskSettings: RiskSettingsDto = {
  maxSingleStockPositionPct: 12,
  maxSectorExposurePct: 35,
  minScoreForNewBuy: 72,
  minSourceQualityForNewBuy: 65,
};

const formatCad = (value?: number) =>
  new Intl.NumberFormat('en-CA', {
    style: 'currency',
    currency: 'CAD',
    maximumFractionDigits: 0,
  }).format(Number(value || 0));

export default function PortfolioPage() {
  const [accounts, setAccounts] = useState<PortfolioAccountDto[]>([]);
  const [holdings, setHoldings] = useState<PortfolioHoldingDto[]>([]);
  const [theses, setTheses] = useState<InvestmentThesisDto[]>([]);
  const [companies, setCompanies] = useState<WatchlistSummaryDto[]>([]);
  const [riskSettings, setRiskSettings] = useState<RiskSettingsDto>(defaultRiskSettings);
  const [report, setReport] = useState<PortfolioActionReportResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [savingAccount, setSavingAccount] = useState(false);
  const [savingHolding, setSavingHolding] = useState(false);
  const [savingThesis, setSavingThesis] = useState(false);
  const [savingRisk, setSavingRisk] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [editingAccountId, setEditingAccountId] = useState<number | null>(null);
  const [editingHoldingId, setEditingHoldingId] = useState<number | null>(null);
  const [editingThesisId, setEditingThesisId] = useState<number | null>(null);

  const [accountName, setAccountName] = useState('Main Portfolio');
  const [accountType, setAccountType] = useState('TAXABLE');
  const [holdingAccountId, setHoldingAccountId] = useState('');
  const [holdingSymbol, setHoldingSymbol] = useState('NVDA');
  const [holdingValue, setHoldingValue] = useState('1000');
  const [holdingQuantity, setHoldingQuantity] = useState('');
  const [thesisCompanyId, setThesisCompanyId] = useState('');
  const [thesisText, setThesisText] = useState('');
  const [buyReason, setBuyReason] = useState('');
  const [horizonMonths, setHorizonMonths] = useState('12');

  const totalValue = useMemo(
    () => holdings.reduce((sum, holding) => sum + Number(holding.marketValueCad || 0), 0),
    [holdings],
  );

  const loadPortfolio = useCallback(async () => {
    setLoading(true);
    try {
      const [accountData, holdingData, thesisData, companyData, settingsData] = await Promise.all([
        apiClient.getPortfolioAccounts(),
        apiClient.getPortfolioHoldings(),
        apiClient.getTheses(),
        apiClient.getWatchlistSummary(),
        apiClient.getRiskSettings(),
      ]);
      setAccounts(accountData);
      setHoldings(holdingData);
      setTheses(thesisData);
      setCompanies(companyData);
      setRiskSettings(settingsData);
      if (!holdingAccountId && accountData.length > 0) {
        setHoldingAccountId(String(accountData[0].id));
      }
      if (!thesisCompanyId && companyData.length > 0) {
        setThesisCompanyId(String(companyData[0].companyId));
      }
    } catch (error) {
      toast.error('Failed to load portfolio workspace');
    } finally {
      setLoading(false);
    }
  }, [holdingAccountId, thesisCompanyId]);

  useEffect(() => {
    void loadPortfolio();
  }, [loadPortfolio]);

  const saveAccount = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedName = accountName.trim();
    if (!trimmedName) {
      toast.error('Account name is required');
      return;
    }

    setSavingAccount(true);
    try {
      const payload = {
        accountName: trimmedName,
        accountType,
        baseCurrency: 'CAD',
      };
      const account = editingAccountId
        ? await apiClient.updatePortfolioAccount(editingAccountId, payload)
        : await apiClient.createPortfolioAccount(payload);
      toast.success(`${editingAccountId ? 'Updated' : 'Created'} ${account.accountName}`);
      setHoldingAccountId(String(account.id));
      setEditingAccountId(null);
      await loadPortfolio();
    } catch (error) {
      toast.error('Failed to save account');
    } finally {
      setSavingAccount(false);
    }
  };

  const saveHolding = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const accountId = Number(holdingAccountId);
    const symbol = holdingSymbol.trim().toUpperCase();
    const marketValueCad = Number(holdingValue);
    if (!accountId || !symbol || !Number.isFinite(marketValueCad) || marketValueCad < 0) {
      toast.error('Enter an account, symbol, and market value');
      return;
    }

    setSavingHolding(true);
    try {
      const payload = {
        accountId,
        symbol,
        marketValueCad,
        quantity: holdingQuantity ? Number(holdingQuantity) : undefined,
      };
      await (editingHoldingId
        ? apiClient.updatePortfolioHolding(editingHoldingId, payload)
        : apiClient.createPortfolioHolding(payload));
      toast.success(`${editingHoldingId ? 'Updated' : 'Saved'} ${symbol} holding`);
      setEditingHoldingId(null);
      await loadPortfolio();
    } catch (error) {
      toast.error('Failed to save holding');
    } finally {
      setSavingHolding(false);
    }
  };

  const saveThesis = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const companyId = Number(thesisCompanyId);
    if (!companyId || thesisText.trim().length < 10) {
      toast.error('Pick a company and write a thesis');
      return;
    }

    setSavingThesis(true);
    try {
      const payload = {
        companyId,
        thesisText: thesisText.trim(),
        buyReason: buyReason.trim() || undefined,
        expectedTimeHorizonMonths: horizonMonths ? Number(horizonMonths) : undefined,
        status: 'ACTIVE',
      };
      await (editingThesisId
        ? apiClient.updateThesis(editingThesisId, payload)
        : apiClient.createThesis(payload));
      toast.success(`Thesis ${editingThesisId ? 'updated' : 'saved'}`);
      setEditingThesisId(null);
      setThesisText('');
      setBuyReason('');
      await loadPortfolio();
    } catch (error) {
      toast.error('Failed to save thesis');
    } finally {
      setSavingThesis(false);
    }
  };

  const editAccount = (account: PortfolioAccountDto) => {
    setEditingAccountId(account.id);
    setAccountName(account.accountName);
    setAccountType(account.accountType);
  };

  const deleteAccount = async (account: PortfolioAccountDto) => {
    if (!window.confirm(`Delete account ${account.accountName}? Holdings in it will also be deleted.`)) {
      return;
    }
    try {
      await apiClient.deletePortfolioAccount(account.id);
      toast.success('Account deleted');
      if (editingAccountId === account.id) {
        cancelAccountEdit();
      }
      await loadPortfolio();
    } catch (error) {
      toast.error('Failed to delete account');
    }
  };

  const cancelAccountEdit = () => {
    setEditingAccountId(null);
    setAccountName('Main Portfolio');
    setAccountType('TAXABLE');
  };

  const editHolding = (holding: PortfolioHoldingDto) => {
    setEditingHoldingId(holding.id);
    setHoldingAccountId(String(holding.accountId));
    setHoldingSymbol(holding.symbol || holding.ticker || '');
    setHoldingValue(String(holding.marketValueCad || ''));
    setHoldingQuantity(holding.quantity ? String(holding.quantity) : '');
  };

  const deleteHolding = async (holding: PortfolioHoldingDto) => {
    if (!window.confirm(`Delete holding ${holding.symbol}?`)) {
      return;
    }
    try {
      await apiClient.deletePortfolioHolding(holding.id);
      toast.success('Holding deleted');
      if (editingHoldingId === holding.id) {
        cancelHoldingEdit();
      }
      await loadPortfolio();
    } catch (error) {
      toast.error('Failed to delete holding');
    }
  };

  const cancelHoldingEdit = () => {
    setEditingHoldingId(null);
    setHoldingSymbol('NVDA');
    setHoldingValue('1000');
    setHoldingQuantity('');
  };

  const editThesis = (thesis: InvestmentThesisDto) => {
    setEditingThesisId(thesis.id);
    setThesisCompanyId(String(thesis.companyId));
    setThesisText(thesis.thesisText);
    setBuyReason(thesis.buyReason || '');
    setHorizonMonths(thesis.expectedTimeHorizonMonths ? String(thesis.expectedTimeHorizonMonths) : '');
  };

  const deleteThesis = async (thesis: InvestmentThesisDto) => {
    if (!window.confirm(`Delete thesis for ${thesis.ticker}?`)) {
      return;
    }
    try {
      await apiClient.deleteThesis(thesis.id);
      toast.success('Thesis deleted');
      if (editingThesisId === thesis.id) {
        cancelThesisEdit();
      }
      await loadPortfolio();
    } catch (error) {
      toast.error('Failed to delete thesis');
    }
  };

  const cancelThesisEdit = () => {
    setEditingThesisId(null);
    setThesisText('');
    setBuyReason('');
    setHorizonMonths('12');
  };

  const saveRiskSettings = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSavingRisk(true);
    try {
      const saved = await apiClient.updateRiskSettings(riskSettings);
      setRiskSettings(saved);
      toast.success('Risk settings saved');
    } catch (error) {
      toast.error('Failed to save risk settings');
    } finally {
      setSavingRisk(false);
    }
  };

  const generateReport = async () => {
    setGenerating(true);
    try {
      const data = await apiClient.generatePortfolioActionReport();
      setReport(data);
      toast.success('Action report generated');
    } catch (error) {
      toast.error('Failed to generate action report');
    } finally {
      setGenerating(false);
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
            <p className="mt-1 text-gray-600">Portfolio actions, thesis tracking, and risk limits.</p>
          </div>
          <nav className="flex flex-wrap gap-2">
            <Link href="/watchlist" className="rounded bg-white px-4 py-2 font-semibold text-gray-800 ring-1 ring-gray-200 hover:bg-gray-100">
              Watchlist
            </Link>
            <Link href="/research" className="rounded bg-white px-4 py-2 font-semibold text-gray-800 ring-1 ring-gray-200 hover:bg-gray-100">
              Research
            </Link>
          </nav>
        </header>

        <section className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="rounded border border-gray-200 bg-white p-4">
            <div className="text-sm text-gray-500">Portfolio Value</div>
            <div className="mt-1 text-2xl font-bold text-gray-950">{formatCad(totalValue)}</div>
          </div>
          <div className="rounded border border-gray-200 bg-white p-4">
            <div className="text-sm text-gray-500">Holdings</div>
            <div className="mt-1 text-2xl font-bold text-gray-950">{holdings.length}</div>
          </div>
          <div className="rounded border border-gray-200 bg-white p-4">
            <div className="text-sm text-gray-500">Active Theses</div>
            <div className="mt-1 text-2xl font-bold text-gray-950">{theses.length}</div>
          </div>
        </section>

        <section className="grid grid-cols-1 gap-6 xl:grid-cols-3">
          <div className="space-y-6 xl:col-span-2">
            <div className="rounded border border-gray-200 bg-white p-6">
              <div className="mb-4 flex items-center justify-between gap-4">
                <h1 className="text-xl font-bold text-gray-950">Action Report</h1>
                <button
                  type="button"
                  onClick={() => void generateReport()}
                  disabled={generating}
                  className="inline-flex items-center justify-center gap-2 rounded bg-gray-900 px-4 py-2 font-semibold text-white hover:bg-gray-800 disabled:bg-gray-500"
                >
                  {generating && <AiOutlineLoading3Quarters className="animate-spin" />}
                  Generate
                </button>
              </div>
              {!report ? (
                <div className="rounded border border-dashed border-gray-300 p-8 text-center text-gray-600">
                  No action report generated yet.
                </div>
              ) : (
                <div className="space-y-3">
                  {report.actions.map((action) => (
                    <article key={action.companyId} className="rounded border border-gray-200 p-4">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div>
                          <div className="text-lg font-bold text-gray-950">
                            {action.ticker} <span className="font-normal text-gray-500">{action.companyName}</span>
                          </div>
                          <div className="mt-1 text-sm text-gray-600">
                            Score {action.stockScore ?? 'n/a'} - Source {action.sourceQualityScore ?? 'n/a'} - Weight {action.currentWeightPct.toFixed(1)}% - Sector {action.sectorExposurePct.toFixed(1)}%
                          </div>
                        </div>
                        <span className="rounded bg-blue-50 px-3 py-1 text-sm font-bold text-blue-800">
                          {action.action}
                        </span>
                      </div>
                      <p className="mt-3 text-sm leading-6 text-gray-700">{action.reason}</p>
                      <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-3">
                        <div className="rounded border border-gray-200 p-3 text-sm">
                          <div className="font-semibold text-gray-950">Buy Trigger</div>
                          <p className="mt-1 text-gray-600">{action.buyTrigger}</p>
                        </div>
                        <div className="rounded border border-gray-200 p-3 text-sm">
                          <div className="font-semibold text-gray-950">Sell Trigger</div>
                          <p className="mt-1 text-gray-600">{action.sellTrigger}</p>
                        </div>
                        <div className="rounded border border-gray-200 p-3 text-sm">
                          <div className="font-semibold text-gray-950">Risk Notes</div>
                          <p className="mt-1 text-gray-600">{action.riskNotes}</p>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </div>

            <div className="rounded border border-gray-200 bg-white p-6">
              <h2 className="mb-4 text-xl font-bold text-gray-950">Holdings</h2>
              {holdings.length === 0 ? (
                <p className="text-sm text-gray-600">No holdings saved yet.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="border-b border-gray-200 text-gray-500">
                      <tr>
                        <th className="py-2 pr-4">Symbol</th>
                        <th className="py-2 pr-4">Account</th>
                        <th className="py-2 pr-4">Value</th>
                        <th className="py-2 pr-4">Weight</th>
                        <th className="py-2 pr-4">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                      {holdings.map((holding) => (
                        <tr key={holding.id}>
                          <td className="py-3 pr-4 font-semibold text-gray-950">{holding.ticker || holding.symbol}</td>
                          <td className="py-3 pr-4 text-gray-700">{holding.accountName}</td>
                          <td className="py-3 pr-4 text-gray-700">{formatCad(holding.marketValueCad)}</td>
                          <td className="py-3 pr-4 text-gray-700">{Number(holding.portfolioWeight || 0).toFixed(1)}%</td>
                          <td className="py-3 pr-4">
                            <div className="flex gap-2">
                              <button
                                type="button"
                                onClick={() => editHolding(holding)}
                                className="text-sm font-semibold text-blue-700 hover:text-blue-900"
                              >
                                Edit
                              </button>
                              <button
                                type="button"
                                onClick={() => void deleteHolding(holding)}
                                className="text-sm font-semibold text-red-700 hover:text-red-900"
                              >
                                Delete
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="rounded border border-gray-200 bg-white p-6">
              <h2 className="mb-4 text-xl font-bold text-gray-950">Theses</h2>
              {theses.length === 0 ? (
                <p className="text-sm text-gray-600">No theses saved yet.</p>
              ) : (
                <div className="space-y-3">
                  {theses.map((thesis) => (
                    <article key={thesis.id} className="rounded border border-gray-200 p-4">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="font-bold text-gray-950">{thesis.ticker} - {thesis.companyName}</div>
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => editThesis(thesis)}
                            className="text-sm font-semibold text-blue-700 hover:text-blue-900"
                          >
                            Edit
                          </button>
                          <button
                            type="button"
                            onClick={() => void deleteThesis(thesis)}
                            className="text-sm font-semibold text-red-700 hover:text-red-900"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                      <p className="mt-2 text-sm leading-6 text-gray-700">{thesis.thesisText}</p>
                      {thesis.buyReason && <p className="mt-2 text-sm text-gray-600">Buy reason: {thesis.buyReason}</p>}
                    </article>
                  ))}
                </div>
              )}
            </div>
          </div>

          <aside className="space-y-6">
            <form onSubmit={saveAccount} className="rounded border border-gray-200 bg-white p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="text-xl font-bold text-gray-950">
                  {editingAccountId ? 'Edit Account' : 'Account'}
                </h2>
                {editingAccountId && (
                  <button
                    type="button"
                    onClick={cancelAccountEdit}
                    className="text-sm font-semibold text-gray-500 hover:text-gray-900"
                  >
                    Cancel
                  </button>
                )}
              </div>
              <label className="mb-2 block text-sm font-semibold text-gray-700" htmlFor="account-name">
                Name
              </label>
              <input
                id="account-name"
                value={accountName}
                onChange={(event) => setAccountName(event.target.value)}
                className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <label className="mb-2 block text-sm font-semibold text-gray-700" htmlFor="account-type">
                Type
              </label>
              <select
                id="account-type"
                value={accountType}
                onChange={(event) => setAccountType(event.target.value)}
                className="mb-4 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="TAXABLE">Taxable</option>
                <option value="TFSA">TFSA</option>
                <option value="RRSP">RRSP</option>
                <option value="PAPER">Paper</option>
              </select>
              <button
                type="submit"
                disabled={savingAccount}
                className="inline-flex w-full items-center justify-center gap-2 rounded bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 disabled:bg-blue-400"
              >
                {savingAccount && <AiOutlineLoading3Quarters className="animate-spin" />}
                {editingAccountId ? 'Update Account' : 'Save Account'}
              </button>
              {accounts.length > 0 && (
                <div className="mt-4 space-y-2 border-t border-gray-200 pt-4">
                  {accounts.map((account) => (
                    <div key={account.id} className="flex items-center justify-between gap-3 text-sm">
                      <div>
                        <div className="font-semibold text-gray-950">{account.accountName}</div>
                        <div className="text-gray-500">{account.accountType} - {account.baseCurrency}</div>
                      </div>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          onClick={() => editAccount(account)}
                          className="font-semibold text-blue-700 hover:text-blue-900"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => void deleteAccount(account)}
                          className="font-semibold text-red-700 hover:text-red-900"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </form>

            <form onSubmit={saveHolding} className="rounded border border-gray-200 bg-white p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="text-xl font-bold text-gray-950">
                  {editingHoldingId ? 'Edit Holding' : 'Holding'}
                </h2>
                {editingHoldingId && (
                  <button
                    type="button"
                    onClick={cancelHoldingEdit}
                    className="text-sm font-semibold text-gray-500 hover:text-gray-900"
                  >
                    Cancel
                  </button>
                )}
              </div>
              <label className="mb-2 block text-sm font-semibold text-gray-700" htmlFor="holding-account">
                Account
              </label>
              <select
                id="holding-account"
                value={holdingAccountId}
                onChange={(event) => setHoldingAccountId(event.target.value)}
                className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Select account</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.accountName}
                  </option>
                ))}
              </select>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-2 block text-sm font-semibold text-gray-700" htmlFor="holding-symbol">
                    Symbol
                  </label>
                  <input
                    id="holding-symbol"
                    value={holdingSymbol}
                    onChange={(event) => setHoldingSymbol(event.target.value.toUpperCase())}
                    className="w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    maxLength={10}
                  />
                </div>
                <div>
                  <label className="mb-2 block text-sm font-semibold text-gray-700" htmlFor="holding-value">
                    Value CAD
                  </label>
                  <input
                    id="holding-value"
                    value={holdingValue}
                    onChange={(event) => setHoldingValue(event.target.value)}
                    className="w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    inputMode="decimal"
                  />
                </div>
              </div>
              <label className="mb-2 mt-3 block text-sm font-semibold text-gray-700" htmlFor="holding-quantity">
                Quantity
              </label>
              <input
                id="holding-quantity"
                value={holdingQuantity}
                onChange={(event) => setHoldingQuantity(event.target.value)}
                className="mb-4 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                inputMode="decimal"
              />
              <button
                type="submit"
                disabled={savingHolding || accounts.length === 0}
                className="inline-flex w-full items-center justify-center gap-2 rounded bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 disabled:bg-blue-400"
              >
                {savingHolding && <AiOutlineLoading3Quarters className="animate-spin" />}
                {editingHoldingId ? 'Update Holding' : 'Save Holding'}
              </button>
            </form>

            <form onSubmit={saveThesis} className="rounded border border-gray-200 bg-white p-6">
              <div className="mb-4 flex items-center justify-between gap-3">
                <h2 className="text-xl font-bold text-gray-950">
                  {editingThesisId ? 'Edit Thesis' : 'Thesis'}
                </h2>
                {editingThesisId && (
                  <button
                    type="button"
                    onClick={cancelThesisEdit}
                    className="text-sm font-semibold text-gray-500 hover:text-gray-900"
                  >
                    Cancel
                  </button>
                )}
              </div>
              <label className="mb-2 block text-sm font-semibold text-gray-700" htmlFor="thesis-company">
                Company
              </label>
              <select
                id="thesis-company"
                value={thesisCompanyId}
                onChange={(event) => setThesisCompanyId(event.target.value)}
                className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Select company</option>
                {companies.map((company) => (
                  <option key={company.companyId} value={company.companyId}>
                    {company.ticker} - {company.name}
                  </option>
                ))}
              </select>
              <textarea
                value={thesisText}
                onChange={(event) => setThesisText(event.target.value)}
                className="mb-3 h-28 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="What must stay true for this investment to work?"
              />
              <input
                value={buyReason}
                onChange={(event) => setBuyReason(event.target.value)}
                className="mb-3 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Buy reason"
              />
              <label className="mb-2 block text-sm font-semibold text-gray-700" htmlFor="thesis-horizon">
                Horizon Months
              </label>
              <input
                id="thesis-horizon"
                value={horizonMonths}
                onChange={(event) => setHorizonMonths(event.target.value)}
                className="mb-4 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                inputMode="numeric"
              />
              <button
                type="submit"
                disabled={savingThesis || companies.length === 0}
                className="inline-flex w-full items-center justify-center gap-2 rounded bg-blue-600 px-4 py-2 font-semibold text-white hover:bg-blue-700 disabled:bg-blue-400"
              >
                {savingThesis && <AiOutlineLoading3Quarters className="animate-spin" />}
                {editingThesisId ? 'Update Thesis' : 'Save Thesis'}
              </button>
            </form>

            <form onSubmit={saveRiskSettings} className="rounded border border-gray-200 bg-white p-6">
              <h2 className="mb-4 text-xl font-bold text-gray-950">Risk Settings</h2>
              {[
                ['maxSingleStockPositionPct', 'Max Stock %'],
                ['maxSectorExposurePct', 'Max Sector %'],
                ['minScoreForNewBuy', 'Min Buy Score'],
                ['minSourceQualityForNewBuy', 'Min Source Quality'],
              ].map(([key, label]) => (
                <label key={key} className="mb-3 block text-sm font-semibold text-gray-700">
                  {label}
                  <input
                    value={riskSettings[key as keyof RiskSettingsDto]}
                    onChange={(event) =>
                      setRiskSettings((current) => ({
                        ...current,
                        [key]: Number(event.target.value),
                      }))
                    }
                    className="mt-2 w-full rounded border border-gray-300 px-3 py-2 text-gray-950 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    inputMode="decimal"
                  />
                </label>
              ))}
              <button
                type="submit"
                disabled={savingRisk}
                className="inline-flex w-full items-center justify-center gap-2 rounded bg-gray-900 px-4 py-2 font-semibold text-white hover:bg-gray-800 disabled:bg-gray-500"
              >
                {savingRisk && <AiOutlineLoading3Quarters className="animate-spin" />}
                Save Risk
              </button>
            </form>
          </aside>
        </section>
      </div>
    </main>
  );
}
