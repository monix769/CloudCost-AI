import React, { useState, useEffect } from 'react';
import {
  DollarSign,
  Wallet,
  Percent,
  TrendingUp,
  TrendingDown,
  Sparkles,
  Filter,
  Download,
  RefreshCw,
  Layers,
  Calendar,
  Cloud,
  ChevronDown,
} from 'lucide-react';
import KpiCard from '../components/KpiCard';
import SpendingTrendChart from '../components/SpendingTrendChart';
import ProviderDistributionChart from '../components/ProviderDistributionChart';
import ServiceBreakdownChart from '../components/ServiceBreakdownChart';
import CostForecastingChart from '../components/CostForecastingChart';
import BudgetUtilizationAnalyticsChart from '../components/BudgetUtilizationAnalyticsChart';
import CostAllocationChart from '../components/CostAllocationChart';
import FinOpsInsightsSection from '../components/FinOpsInsightsSection';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import costAnalyticsService from '../services/costAnalyticsService';
import cloudAccountService from '../services/cloudAccountService';
import { formatCurrency } from '../utils/formatters';

export default function AnalyticsPage() {
  const [provider, setProvider] = useState('ALL');
  const [currency, setCurrency] = useState('USD');
  const [datePreset, setDatePreset] = useState('30D');
  const [accounts, setAccounts] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState('ALL');

  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshKey, setRefreshKey] = useState(0);
  const [exportOpen, setExportOpen] = useState(false);

  // Fetch accounts for filter dropdown
  useEffect(() => {
    async function fetchAccounts() {
      try {
        const list = await cloudAccountService.getAllAccounts();
        setAccounts(list || []);
      } catch (e) {
        console.warn('Failed to load cloud accounts for filter:', e);
      }
    }
    fetchAccounts();
  }, []);

  // Fetch executive overview
  useEffect(() => {
    async function fetchOverview() {
      try {
        setLoading(true);
        const data = await costAnalyticsService.getOverview({ currency });
        setOverview(data);
      } catch (err) {
        console.error('Failed to load FinOps overview analytics:', err);
      } finally {
        setLoading(false);
      }
    }
    fetchOverview();
  }, [currency, refreshKey]);

  const summary = overview?.summary;
  const mom = overview?.monthOverMonth;
  const budget = overview?.budgetHealth;

  const totalCost = summary?.totalCost || 0;
  const currentMonthCost = summary?.currentMonthCost || 0;
  const avgDaily = summary?.averageDailyCost || 0;
  const momPct = mom?.percentageChange || 0;
  const momDirection = mom?.direction || 'NO_CHANGE';
  const budgetUtilization = budget?.overallUtilizationPercentage || 0;

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Transparency Disclaimer */}
      <DemoDataDisclaimer />

      {/* Top Filter Toolbar */}
      <div className="glass-panel rounded-2xl p-4 border border-white/[0.08] shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Filter controls */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Provider Filter */}
          <div className="flex items-center space-x-2 text-xs text-slate-400 font-mono">
            <Filter className="w-3.5 h-3.5 text-blue-400" />
            <span>Provider:</span>
          </div>
          <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono">
            {['ALL', 'AWS', 'AZURE', 'GCP'].map((p) => (
              <button
                key={p}
                onClick={() => setProvider(p)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                  provider === p
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {p}
              </button>
            ))}
          </div>

          {/* Cloud Account Filter */}
          {accounts.length > 0 && (
            <div className="hidden sm:flex items-center space-x-2 text-xs font-mono">
              <span className="text-slate-400">Account:</span>
              <select
                value={selectedAccountId}
                onChange={(e) => setSelectedAccountId(e.target.value)}
                className="bg-navy-900 border border-slate-800 rounded-xl px-2.5 py-1 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
              >
                <option value="ALL">All Accounts</option>
                {accounts.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.accountName} ({a.provider})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Currency Filter */}
          <div className="flex items-center space-x-2 text-xs font-mono ml-1">
            <span className="text-slate-400">Currency:</span>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              className="bg-navy-900 border border-slate-800 rounded-xl px-2.5 py-1 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
            >
              <option value="USD">USD ($)</option>
              <option value="EUR">EUR (€)</option>
              <option value="GBP">GBP (£)</option>
              <option value="INR">INR (₹)</option>
            </select>
          </div>
        </div>

        {/* Action Buttons: Refresh & Export */}
        <div className="flex items-center space-x-2 self-start md:self-auto relative">
          <button
            onClick={() => setRefreshKey((k) => k + 1)}
            disabled={loading}
            className="p-2 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 transition-colors"
            title="Refresh analytics data"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-blue-400 ${loading ? 'animate-spin' : ''}`} />
          </button>

          {/* Export Dropdown */}
          <div className="relative">
            <button
              onClick={() => setExportOpen((o) => !o)}
              className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-sm transition-colors"
            >
              <Download className="w-3.5 h-3.5" />
              <span>Export Reports</span>
              <ChevronDown className="w-3.5 h-3.5" />
            </button>

            {exportOpen && (
              <div className="absolute right-0 mt-2 w-52 rounded-xl bg-navy-900 border border-slate-800 shadow-xl z-30 py-1.5 text-xs font-mono">
                <button
                  onClick={() => {
                    costAnalyticsService.downloadExport('spending-trends', { currency });
                    setExportOpen(false);
                  }}
                  className="w-full text-left px-3 py-2 hover:bg-slate-800/60 text-slate-200"
                >
                  Spending Trends (CSV)
                </button>
                <button
                  onClick={() => {
                    costAnalyticsService.downloadExport('provider-comparison', { currency });
                    setExportOpen(false);
                  }}
                  className="w-full text-left px-3 py-2 hover:bg-slate-800/60 text-slate-200"
                >
                  Provider Distribution (CSV)
                </button>
                <button
                  onClick={() => {
                    costAnalyticsService.downloadExport('service-breakdown', { currency });
                    setExportOpen(false);
                  }}
                  className="w-full text-left px-3 py-2 hover:bg-slate-800/60 text-slate-200"
                >
                  Service Breakdown (CSV)
                </button>
                <button
                  onClick={() => {
                    costAnalyticsService.downloadExport('forecast', { horizon: '30D', currency });
                    setExportOpen(false);
                  }}
                  className="w-full text-left px-3 py-2 hover:bg-slate-800/60 text-slate-200"
                >
                  Forecast Projections (CSV)
                </button>
                <button
                  onClick={() => {
                    costAnalyticsService.downloadExport('insights', { currency });
                    setExportOpen(false);
                  }}
                  className="w-full text-left px-3 py-2 hover:bg-slate-800/60 text-slate-200"
                >
                  FinOps Insights (CSV)
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 6 Executive FinOps KPI Cards (Requirement 14.A) */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        {/* KPI 1: Total Cloud Spending */}
        <KpiCard
          title="Total Cloud Spend"
          value={formatCurrency(totalCost, currency)}
          subtitle="Cumulative across all accounts"
          icon={DollarSign}
          accentColor="blue"
          badgeText="ALL-TIME"
        />

        {/* KPI 2: Current Month Spending */}
        <KpiCard
          title="Current Month Spend"
          value={formatCurrency(currentMonthCost, currency)}
          subtitle="Calendar month to date"
          icon={Calendar}
          accentColor="indigo"
          badgeText="MTD"
        />

        {/* KPI 3: Month-over-Month Change */}
        <KpiCard
          title="MoM Spend Delta"
          value={`${momPct >= 0 ? '+' : ''}${momPct}%`}
          subtitle={`${momDirection === 'INCREASE' ? 'Increased' : 'Decreased'} vs prev month`}
          changePercent={Math.abs(momPct)}
          changeType={momDirection === 'INCREASE' ? 'increase' : 'decrease'}
          icon={momDirection === 'INCREASE' ? TrendingUp : TrendingDown}
          accentColor={momDirection === 'INCREASE' ? 'rose' : 'emerald'}
          badgeText="MoM"
        />

        {/* KPI 4: Average Daily Spending */}
        <KpiCard
          title="Average Daily Spend"
          value={formatCurrency(avgDaily, currency)}
          subtitle="Run rate velocity"
          icon={TrendingUp}
          accentColor="sky"
          badgeText="DAILY"
        />

        {/* KPI 5: Forecasted Monthly Spending */}
        <KpiCard
          title="Forecasted Month EOP"
          value={formatCurrency(budget?.estimatedEndOfPeriodSpend || (avgDaily * 30), currency)}
          subtitle="Based on current burn rate"
          icon={Sparkles}
          accentColor="purple"
          badgeText="PROJECTED"
        />

        {/* KPI 6: Budget Utilization */}
        <KpiCard
          title="Budget Utilization"
          value={`${budgetUtilization}%`}
          subtitle={`Limit: ${formatCurrency(budget?.totalAllocatedBudget || 0, currency)}`}
          progress={budgetUtilization}
          progressColor={budgetUtilization >= 90 ? 'bg-rose-500' : budgetUtilization >= 70 ? 'bg-amber-500' : 'bg-emerald-500'}
          icon={Percent}
          accentColor="emerald"
          badgeText={budget?.overallHealthStatus || 'HEALTHY'}
        />
      </section>

      {/* Primary Analytics Charts Row: Trends & Provider Distribution */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <SpendingTrendChart currency={currency} provider={provider} />
        <ProviderDistributionChart />
      </section>

      {/* Service Breakdown & Multi-Dimensional Allocation Row */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ServiceBreakdownChart />
        <CostAllocationChart currency={currency} />
      </section>

      {/* Statistical Forecasting Engine (Requirement 14.E) */}
      <section>
        <CostForecastingChart currency={currency} />
      </section>

      {/* Budget Utilization & Run Rate (Requirement 14.F) */}
      <section>
        <BudgetUtilizationAnalyticsChart currency={currency} />
      </section>

      {/* Actionable FinOps Insights Panel (Requirement 14.H) */}
      <section>
        <FinOpsInsightsSection currency={currency} />
      </section>
    </div>
  );
}

