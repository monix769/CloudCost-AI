import React, { useState, useEffect, useMemo } from 'react';
import { optimizationService } from '../services/optimizationService';
import { formatCurrency, formatDate } from '../utils/formatters';
import { Bar, Doughnut } from 'react-chartjs-2';
import '../utils/chartConfig';
import {
  Sparkles,
  TrendingDown,
  DollarSign,
  Cpu,
  HardDrive,
  Calendar,
  Layers,
  CheckCircle,
  AlertTriangle,
  Clock,
  Filter,
  Search,
  RefreshCw,
  Download,
  X,
  Check,
  ChevronRight,
  Shield,
  Activity,
  ArrowRight,
  Info,
  Server,
  Zap,
  Sliders
} from 'lucide-react';

export default function CostOptimizationPage() {
  // Navigation tabs
  const [activeTab, setActiveTab] = useState('RECOMMENDATIONS'); // RECOMMENDATIONS, COMMITMENT_PLANNER, RESOURCE_TELEMETRY

  // Primary data states
  const [overview, setOverview] = useState(null);
  const [savingsSummary, setSavingsSummary] = useState(null);
  const [recommendations, setRecommendations] = useState([]);
  const [commitmentData, setCommitmentData] = useState(null);
  const [telemetrySnapshots, setTelemetrySnapshots] = useState([]);

  // Loading and feedback states
  const [loading, setLoading] = useState(true);
  const [scanning, setScanning] = useState(false);
  const [scanMessage, setScanMessage] = useState(null);
  const [error, setError] = useState(null);

  // Filters for Recommendations
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [selectedProvider, setSelectedProvider] = useState('ALL');
  const [selectedPriority, setSelectedPriority] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Drawer / Detail modal state
  const [selectedRec, setSelectedRec] = useState(null);
  const [recHistory, setRecHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [actionNotes, setActionNotes] = useState('');
  const [actionStatus, setActionStatus] = useState('');
  const [realizedSavingsInput, setRealizedSavingsInput] = useState('');
  const [submittingAction, setSubmittingAction] = useState(false);

  // Fetch all initial data
  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [ovData, sumData, recPage, commData, telPage] = await Promise.all([
        optimizationService.getOverview().catch(() => null),
        optimizationService.getSavingsSummary().catch(() => null),
        optimizationService.getRecommendations({ size: 100 }).catch(() => ({ content: [] })),
        optimizationService.getCommitmentAnalysis().catch(() => null),
        optimizationService.getResourceUtilization({ size: 50 }).catch(() => ({ content: [] }))
      ]);

      setOverview(ovData);
      setSavingsSummary(sumData);
      setRecommendations(recPage?.content || []);
      setCommitmentData(commData);
      setTelemetrySnapshots(telPage?.content || []);
    } catch (err) {
      console.error('Failed to load optimization data:', err);
      setError(err.message || 'Failed to fetch optimization data');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  // Filter recommendations
  const filteredRecommendations = useMemo(() => {
    return recommendations.filter((item) => {
      if (selectedCategory !== 'ALL' && item.recommendationType !== selectedCategory) return false;
      if (selectedProvider !== 'ALL' && item.cloudProvider !== selectedProvider) return false;
      if (selectedPriority !== 'ALL' && item.priority !== selectedPriority) return false;
      if (selectedStatus !== 'ALL' && item.status !== selectedStatus) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const rId = (item.resourceId || '').toLowerCase();
        const rName = (item.resourceName || '').toLowerCase();
        const rType = (item.resourceType || '').toLowerCase();
        if (!rId.includes(q) && !rName.includes(q) && !rType.includes(q)) return false;
      }
      return true;
    });
  }, [recommendations, selectedCategory, selectedProvider, selectedPriority, selectedStatus, searchQuery]);

  // Handle manual analysis scan trigger
  const handleTriggerAnalysis = async () => {
    try {
      setScanning(true);
      setScanMessage(null);
      const res = await optimizationService.runOptimizationAnalysis();
      const created = res?.newRecommendationsCreated ?? 0;
      const updated = res?.existingRecommendationsUpdated ?? 0;
      setScanMessage(`Scan complete: ${created} new opportunities identified, ${updated} refreshed.`);
      await loadDashboardData();
    } catch (err) {
      setScanMessage(`Scan failed: ${err.message || 'Error executing optimization engine'}`);
    } finally {
      setScanning(false);
    }
  };

  // Handle CSV export
  const handleExportCsv = async () => {
    try {
      await optimizationService.exportCsv();
    } catch (err) {
      alert(`Export failed: ${err.message}`);
    }
  };

  // Open detail drawer and load audit history
  const handleOpenDrawer = async (rec) => {
    setSelectedRec(rec);
    setActionStatus(rec.status);
    setActionNotes('');
    setRealizedSavingsInput(rec.realizedMonthlySavings > 0 ? rec.realizedMonthlySavings : rec.estimatedMonthlySavings);
    try {
      setHistoryLoading(true);
      const hist = await optimizationService.getRecommendationHistory(rec.id);
      setRecHistory(hist);
    } catch (err) {
      console.warn('Failed to load recommendation history:', err);
      setRecHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  // Close drawer
  const handleCloseDrawer = () => {
    setSelectedRec(null);
    setRecHistory([]);
  };

  // Submit status update
  const handleUpdateStatus = async (targetStatus) => {
    if (!selectedRec) return;
    try {
      setSubmittingAction(true);
      const realizedVal = targetStatus === 'IMPLEMENTED' && realizedSavingsInput
        ? parseFloat(realizedSavingsInput)
        : null;

      const updated = await optimizationService.updateStatus(
        selectedRec.id,
        targetStatus,
        actionNotes,
        realizedVal
      );

      // Refresh local list
      setRecommendations((prev) =>
        prev.map((r) => (r.id === updated.id ? updated : r))
      );
      setSelectedRec(updated);

      // Refresh history & overview
      const hist = await optimizationService.getRecommendationHistory(updated.id);
      setRecHistory(hist);
      const newOv = await optimizationService.getOverview();
      setOverview(newOv);
      setActionNotes('');
    } catch (err) {
      alert(`Failed to update status: ${err.message}`);
    } finally {
      setSubmittingAction(false);
    }
  };

  // Helpers for Badges
  const getPriorityBadge = (priority) => {
    switch (priority) {
      case 'CRITICAL':
        return 'bg-rose-500/15 text-rose-300 border-rose-500/30';
      case 'HIGH':
        return 'bg-amber-500/15 text-amber-300 border-amber-500/30';
      case 'MEDIUM':
        return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
      case 'LOW':
        return 'bg-slate-500/15 text-slate-300 border-slate-500/30';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'NEW':
        return 'bg-purple-500/15 text-purple-300 border-purple-500/30';
      case 'REVIEWED':
        return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
      case 'APPROVED':
        return 'bg-cyan-500/15 text-cyan-300 border-cyan-500/30';
      case 'IMPLEMENTED':
        return 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30';
      case 'REJECTED':
        return 'bg-rose-500/15 text-rose-400 border-rose-500/30';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  const getProviderBadge = (provider) => {
    switch (provider) {
      case 'AWS':
        return 'bg-amber-500/10 text-amber-300 border-amber-500/20';
      case 'AZURE':
        return 'bg-sky-500/10 text-sky-300 border-sky-500/20';
      case 'GCP':
        return 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20';
      default:
        return 'bg-slate-800 text-slate-400 border-slate-700';
    }
  };

  // Chart Data: Savings Opportunity by Provider
  const providerChartData = useMemo(() => {
    const byProv = savingsSummary?.byProvider || {};
    const labels = Object.keys(byProv);
    const dataValues = Object.values(byProv);

    return {
      labels: labels.length ? labels : ['No Data'],
      datasets: [
        {
          label: 'Monthly Savings ($)',
          data: dataValues.length ? dataValues : [0],
          backgroundColor: [
            'rgba(245, 158, 11, 0.75)', // AWS Amber
            'rgba(14, 165, 233, 0.75)', // Azure Sky
            'rgba(16, 185, 129, 0.75)', // GCP Emerald
            'rgba(168, 85, 247, 0.75)',
          ],
          borderColor: [
            '#f59e0b',
            '#0ea5e9',
            '#10b981',
            '#a855f7',
          ],
          borderWidth: 1.5,
          borderRadius: 8,
        },
      ],
    };
  }, [savingsSummary]);

  // Chart Data: Savings by Recommendation Type
  const typeChartData = useMemo(() => {
    const byType = savingsSummary?.byRecommendationType || {};
    const labels = Object.keys(byType).map((k) => k.replace(/_/g, ' '));
    const dataValues = Object.values(byType);

    return {
      labels: labels.length ? labels : ['No Data'],
      datasets: [
        {
          data: dataValues.length ? dataValues : [1],
          backgroundColor: [
            'rgba(59, 130, 246, 0.8)', // Rightsizing
            'rgba(244, 63, 94, 0.8)',  // Idle Resource
            'rgba(168, 85, 247, 0.8)', // Commitment
            'rgba(16, 185, 129, 0.8)', // Modernization
          ],
          borderColor: '#0f172a',
          borderWidth: 2,
        },
      ],
    };
  }, [savingsSummary]);

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Hero Header Strip */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/[0.08] shadow-card-elevated relative overflow-hidden flex flex-col lg:flex-row lg:items-center justify-between gap-6">
        <div className="space-y-2 max-w-2xl">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-purple-500/15 border border-purple-500/30 text-purple-300 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5" />
            <span>FinOps Automated Cost Optimization & Rightsizing Engine</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Cloud Cost Optimization & Rightsizing
          </h1>
          <p className="text-xs sm:text-sm text-slate-300 leading-relaxed">
            Continuously evaluate compute utilization telemetry, identify abandoned resources, and model Reserved Instance / Savings Plan commitments with explainable, non-destructive recommendations.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center gap-3 shrink-0">
          <button
            onClick={handleTriggerAnalysis}
            disabled={scanning}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white shadow-lg transition-all flex items-center space-x-2 disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${scanning ? 'animate-spin' : ''}`} />
            <span>{scanning ? 'Running FinOps Analysis...' : 'Run Optimization Scan'}</span>
          </button>

          <button
            onClick={handleExportCsv}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-navy-800 hover:bg-navy-700 text-slate-200 border border-slate-700 transition-all flex items-center space-x-2"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Scan Feedback Banner */}
      {scanMessage && (
        <div className="p-3.5 rounded-2xl bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Info className="w-4 h-4 shrink-0" />
            <span>{scanMessage}</span>
          </div>
          <button onClick={() => setScanMessage(null)} className="hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Potential Monthly Savings */}
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-2">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-mono font-bold uppercase tracking-wider">Potential Monthly Savings</span>
            <DollarSign className="w-4 h-4 text-purple-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold font-mono text-purple-400">
            {formatCurrency(overview?.totalPotentialMonthlySavings || 0)}
            <span className="text-xs text-slate-400 font-normal"> / mo</span>
          </div>
          <div className="text-[11px] text-slate-400 font-mono">
            Across {overview?.totalRecommendationsCount || 0} active optimization opportunities
          </div>
        </div>

        {/* Projected Annual Savings */}
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-2">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-mono font-bold uppercase tracking-wider">Projected Annual Savings</span>
            <TrendingDown className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold font-mono text-emerald-400">
            {formatCurrency(overview?.totalPotentialAnnualSavings || 0)}
            <span className="text-xs text-slate-400 font-normal"> / yr</span>
          </div>
          <div className="text-[11px] text-emerald-400/80 font-mono font-semibold">
            Modeled 12-month annualized reduction
          </div>
        </div>

        {/* Realized Monthly Savings */}
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-2">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-mono font-bold uppercase tracking-wider">Realized Monthly Savings</span>
            <CheckCircle className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold font-mono text-cyan-400">
            {formatCurrency(overview?.totalRealizedMonthlySavings || 0)}
            <span className="text-xs text-slate-400 font-normal"> / mo</span>
          </div>
          <div className="text-[11px] text-slate-400 font-mono">
            {overview?.implementedRecommendationsCount || 0} recommendations successfully implemented
          </div>
        </div>

        {/* High & Critical Priority */}
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-2">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-mono font-bold uppercase tracking-wider">High & Critical Priority</span>
            <AlertTriangle className="w-4 h-4 text-amber-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold font-mono text-amber-400">
            {(overview?.criticalPriorityCount || 0) + (overview?.highPriorityCount || 0)}
            <span className="text-xs text-slate-400 font-normal"> urgent items</span>
          </div>
          <div className="text-[11px] text-amber-400/80 font-mono">
            {overview?.criticalPriorityCount || 0} Critical, {overview?.highPriorityCount || 0} High priority
          </div>
        </div>
      </div>

      {/* Visual Analytics Strip (Dual Charts) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        {/* Savings by Cloud Provider */}
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-white flex items-center space-x-2">
              <Layers className="w-4 h-4 text-blue-400" />
              <span>Monthly Savings Opportunity by Cloud Provider</span>
            </h3>
            <span className="text-[10px] font-mono text-slate-400">USD</span>
          </div>
          <div className="h-56">
            <Bar
              data={providerChartData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                  y: {
                    beginAtZero: true,
                    ticks: { callback: (val) => `$${val}` },
                    grid: { color: 'rgba(255, 255, 255, 0.05)' },
                  },
                  x: {
                    grid: { display: false },
                  },
                },
              }}
            />
          </div>
        </div>

        {/* Savings by Recommendation Category */}
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold text-white flex items-center space-x-2">
              <Sliders className="w-4 h-4 text-purple-400" />
              <span>Savings Breakdown by Recommendation Type</span>
            </h3>
            <span className="text-[10px] font-mono text-slate-400">Category Share</span>
          </div>
          <div className="h-56 flex items-center justify-center">
            <Doughnut
              data={typeChartData}
              options={{
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                  legend: {
                    position: 'right',
                    labels: { color: '#cbd5e1', font: { size: 10 } },
                  },
                },
                cutout: '70%',
              }}
            />
          </div>
        </div>
      </div>

      {/* Navigation Sub-Tabs */}
      <div className="flex border-b border-slate-800 space-x-4">
        {[
          { id: 'RECOMMENDATIONS', label: 'Optimization Recommendations', icon: Zap, count: recommendations.length },
          { id: 'COMMITMENT_PLANNER', label: 'Commitment Planner (RIs & Savings Plans)', icon: Calendar },
          { id: 'RESOURCE_TELEMETRY', label: 'Resource Telemetry & Monitoring', icon: Activity, count: telemetrySnapshots.length },
        ].map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`pb-3 px-2 text-xs font-semibold flex items-center space-x-2 transition-all border-b-2 font-mono ${
                isActive
                  ? 'border-blue-500 text-blue-400'
                  : 'border-transparent text-slate-400 hover:text-slate-200'
              }`}
            >
              <Icon className="w-4 h-4" />
              <span>{tab.label}</span>
              {tab.count !== undefined && (
                <span className={`px-2 py-0.5 rounded-full text-[10px] ${
                  isActive ? 'bg-blue-500/20 text-blue-300' : 'bg-slate-800 text-slate-400'
                }`}>
                  {tab.count}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* TAB 1: RECOMMENDATIONS */}
      {activeTab === 'RECOMMENDATIONS' && (
        <div className="space-y-4">
          {/* Filter Bar */}
          <div className="glass-panel rounded-2xl p-4 border border-white/[0.08] shadow-card-elevated flex flex-wrap items-center justify-between gap-4">
            <div className="flex flex-wrap items-center gap-2">
              {['ALL', 'RIGHTSIZING', 'IDLE_RESOURCE', 'COMMITMENT_SAVINGS_PLAN'].map((cat) => (
                <button
                  key={cat}
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition-all font-mono ${
                    selectedCategory === cat
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'bg-navy-900 text-slate-400 hover:text-slate-200 border border-slate-800'
                  }`}
                >
                  {cat.replace(/_/g, ' ')}
                </button>
              ))}
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {/* Provider Filter */}
              <select
                value={selectedProvider}
                onChange={(e) => setSelectedProvider(e.target.value)}
                className="px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-slate-300 font-mono focus:outline-none focus:border-blue-500"
              >
                <option value="ALL">All Providers</option>
                <option value="AWS">AWS</option>
                <option value="AZURE">Azure</option>
                <option value="GCP">GCP</option>
              </select>

              {/* Priority Filter */}
              <select
                value={selectedPriority}
                onChange={(e) => setSelectedPriority(e.target.value)}
                className="px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-slate-300 font-mono focus:outline-none focus:border-blue-500"
              >
                <option value="ALL">All Priorities</option>
                <option value="CRITICAL">Critical</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>

              {/* Status Filter */}
              <select
                value={selectedStatus}
                onChange={(e) => setSelectedStatus(e.target.value)}
                className="px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-slate-300 font-mono focus:outline-none focus:border-blue-500"
              >
                <option value="ALL">All Statuses</option>
                <option value="NEW">New</option>
                <option value="REVIEWED">Reviewed</option>
                <option value="APPROVED">Approved</option>
                <option value="IMPLEMENTED">Implemented</option>
                <option value="REJECTED">Rejected</option>
              </select>

              {/* Search Box */}
              <div className="relative">
                <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search resource..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8 pr-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-white placeholder-slate-500 font-mono focus:outline-none focus:border-blue-500 w-44 sm:w-56"
                />
              </div>
            </div>
          </div>

          {/* Recommendations Table */}
          {loading ? (
            <div className="p-12 text-center text-slate-400 space-y-2">
              <RefreshCw className="w-6 h-6 animate-spin mx-auto text-blue-500" />
              <p className="text-xs">Loading optimization opportunities...</p>
            </div>
          ) : filteredRecommendations.length === 0 ? (
            <div className="glass-panel rounded-2xl p-12 text-center border border-white/[0.08] shadow-card-elevated space-y-3">
              <CheckCircle className="w-10 h-10 text-emerald-400 mx-auto" />
              <h4 className="text-base font-bold text-white">No Recommendations Match Current Criteria</h4>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                No optimization recommendations were found matching your current filter filters. Run an optimization scan or broaden your search.
              </p>
            </div>
          ) : (
            <div className="glass-panel rounded-2xl border border-white/[0.08] shadow-card-elevated overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-navy-900/80 border-b border-slate-800 text-slate-400 uppercase font-mono text-[10px] tracking-wider">
                    <tr>
                      <th className="py-3 px-4">Resource & Provider</th>
                      <th className="py-3 px-4">Category & Priority</th>
                      <th className="py-3 px-4">Current → Suggested Config</th>
                      <th className="py-3 px-4">Monthly Savings</th>
                      <th className="py-3 px-4">Confidence</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/80 text-slate-300">
                    {filteredRecommendations.map((rec) => (
                      <tr key={rec.id} className="hover:bg-white/[0.02] transition-colors">
                        {/* Resource */}
                        <td className="py-3.5 px-4">
                          <div className="space-y-1">
                            <div className="flex items-center space-x-2">
                              <span className={`text-[10px] font-mono font-bold px-1.5 py-0.5 rounded border ${getProviderBadge(rec.cloudProvider)}`}>
                                {rec.cloudProvider}
                              </span>
                              <span className="font-bold text-white text-xs">{rec.resourceName || rec.resourceId}</span>
                            </div>
                            <div className="text-[11px] font-mono text-slate-400 truncate max-w-xs">
                              {rec.resourceId}
                            </div>
                          </div>
                        </td>

                        {/* Category & Priority */}
                        <td className="py-3.5 px-4">
                          <div className="space-y-1">
                            <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700 block w-max">
                              {rec.recommendationType.replace(/_/g, ' ')}
                            </span>
                            <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border block w-max ${getPriorityBadge(rec.priority)}`}>
                              {rec.priority}
                            </span>
                          </div>
                        </td>

                        {/* Config Comparison */}
                        <td className="py-3.5 px-4">
                          <div className="space-y-1 max-w-xs">
                            <div className="text-[11px] font-mono text-slate-400 line-through">
                              {rec.currentConfig}
                            </div>
                            <div className="text-xs font-semibold text-emerald-400 flex items-center space-x-1">
                              <ArrowRight className="w-3 h-3 shrink-0" />
                              <span className="truncate">{rec.suggestedConfig}</span>
                            </div>
                          </div>
                        </td>

                        {/* Monthly Savings */}
                        <td className="py-3.5 px-4">
                          <div className="space-y-0.5">
                            <div className="text-sm font-bold font-mono text-emerald-400">
                              +{formatCurrency(rec.estimatedMonthlySavings)}
                              <span className="text-[10px] text-slate-400 font-normal"> / mo</span>
                            </div>
                            <div className="text-[10px] font-mono text-slate-400">
                              ~{formatCurrency(rec.estimatedAnnualSavings)}/yr ({rec.savingsPercentage || 0}%)
                            </div>
                          </div>
                        </td>

                        {/* Confidence */}
                        <td className="py-3.5 px-4 font-mono text-[11px]">
                          <span className={`px-2 py-0.5 rounded ${
                            rec.confidenceLevel === 'HIGH' ? 'bg-emerald-500/10 text-emerald-300' : 'bg-slate-800 text-slate-300'
                          }`}>
                            {rec.confidenceLevel}
                          </span>
                        </td>

                        {/* Status */}
                        <td className="py-3.5 px-4">
                          <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getStatusBadge(rec.status)}`}>
                            {rec.status}
                          </span>
                        </td>

                        {/* Actions */}
                        <td className="py-3.5 px-4 text-right">
                          <div className="flex items-center justify-end space-x-2">
                            <button
                              onClick={() => handleOpenDrawer(rec)}
                              className="px-2.5 py-1.5 rounded-lg text-xs font-semibold bg-navy-800 hover:bg-navy-700 text-slate-200 border border-slate-700 transition-all flex items-center space-x-1"
                            >
                              <span>Details</span>
                              <ChevronRight className="w-3 h-3" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: COMMITMENT PLANNER */}
      {activeTab === 'COMMITMENT_PLANNER' && (
        <div className="space-y-5">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
            <div className="flex items-start justify-between">
              <div>
                <h3 className="text-base font-bold text-white flex items-center space-x-2">
                  <Calendar className="w-4 h-4 text-purple-400" />
                  <span>Reserved Instances & Savings Plans Commitment Engine</span>
                </h3>
                <p className="text-xs text-slate-300 mt-1 max-w-2xl leading-relaxed">
                  Analyze historical 30-day continuous compute workloads across AWS, Azure, and GCP. Lock in substantial discounts by committing to minimum baseline compute thresholds without upfront hardware procurement.
                </p>
              </div>

              <div className="p-3 rounded-xl bg-navy-900 border border-purple-500/30 text-right">
                <span className="text-[10px] font-mono uppercase text-slate-400 block">Evaluated Compute Baseline</span>
                <span className="text-xl font-bold font-mono text-purple-400">
                  {formatCurrency(commitmentData?.baselineMonthlyComputeSpend || 0)} / mo
                </span>
              </div>
            </div>

            {/* Commitment Plan Comparison Cards */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
              {/* 1-Year Commitment */}
              <div className="p-5 rounded-2xl bg-navy-900/60 border border-blue-500/30 space-y-3 relative overflow-hidden">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-blue-400 font-mono uppercase">1-Year No-Upfront Commitment</span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold font-mono bg-blue-500/20 text-blue-300 border border-blue-500/30">
                    28% Guaranteed Discount
                  </span>
                </div>
                <div className="space-y-1">
                  <span className="text-[11px] font-mono text-slate-400 block">Recommended Monthly Commitment:</span>
                  <span className="text-2xl font-extrabold font-mono text-white">
                    {formatCurrency(commitmentData?.recommended1YearSavingsPlanMonthly || 0)}
                    <span className="text-xs text-slate-400 font-normal"> / mo</span>
                  </span>
                </div>
                <div className="pt-2 border-t border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-mono text-slate-400 block">Monthly Savings</span>
                    <span className="text-base font-bold font-mono text-emerald-400">
                      +{formatCurrency(commitmentData?.estimated1YearMonthlySavings || 0)}/mo
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] font-mono text-slate-400 block">Annual Savings</span>
                    <span className="text-base font-bold font-mono text-emerald-400">
                      +{formatCurrency(commitmentData?.estimated1YearAnnualSavings || 0)}/yr
                    </span>
                  </div>
                </div>
              </div>

              {/* 3-Year Commitment */}
              <div className="p-5 rounded-2xl bg-navy-900/60 border border-purple-500/30 space-y-3 relative overflow-hidden">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-purple-400 font-mono uppercase">3-Year No-Upfront Commitment</span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold font-mono bg-purple-500/20 text-purple-300 border border-purple-500/30">
                    42% Maximum Discount
                  </span>
                </div>
                <div className="space-y-1">
                  <span className="text-[11px] font-mono text-slate-400 block">Recommended Monthly Commitment:</span>
                  <span className="text-2xl font-extrabold font-mono text-white">
                    {formatCurrency(commitmentData?.recommended3YearSavingsPlanMonthly || 0)}
                    <span className="text-xs text-slate-400 font-normal"> / mo</span>
                  </span>
                </div>
                <div className="pt-2 border-t border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-mono text-slate-400 block">Monthly Savings</span>
                    <span className="text-base font-bold font-mono text-emerald-400">
                      +{formatCurrency(commitmentData?.estimated3YearMonthlySavings || 0)}/mo
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-[10px] font-mono text-slate-400 block">Annual Savings</span>
                    <span className="text-base font-bold font-mono text-emerald-400">
                      +{formatCurrency(commitmentData?.estimated3YearAnnualSavings || 0)}/yr
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* FinOps Assumptions List */}
            <div className="p-4 rounded-xl bg-navy-950/60 border border-slate-800/80 space-y-2">
              <span className="text-[11px] font-mono font-bold text-slate-300 uppercase flex items-center space-x-1.5">
                <Shield className="w-3.5 h-3.5 text-blue-400" />
                <span>Explicit FinOps Assumptions & Underwriting Rules</span>
              </span>
              <ul className="text-xs text-slate-400 space-y-1 list-disc list-inside">
                {(commitmentData?.assumptions || [
                  'Evaluated continuous 30-day compute expenditure across EC2, Azure VMs, and GCP Compute Engine.',
                  'Conservative 75% coverage target applied to stable baseline to minimize over-commitment risk.',
                  'Assumes 1-Year / 3-Year No-Upfront Compute Savings Plans or Standard Reserved Instances.',
                  'Prices calculated relative to on-demand retail rates without custom enterprise agreement discounts.'
                ]).map((asmp, idx) => (
                  <li key={idx} className="leading-relaxed">{asmp}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: RESOURCE TELEMETRY */}
      {activeTab === 'RESOURCE_TELEMETRY' && (
        <div className="space-y-4">
          <div className="glass-panel rounded-2xl border border-white/[0.08] shadow-card-elevated overflow-hidden">
            <div className="p-4 bg-navy-900/50 border-b border-slate-800 flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-white flex items-center space-x-2">
                  <Activity className="w-4 h-4 text-emerald-400" />
                  <span>Monitored Resource Utilization Telemetry</span>
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Genuine CPU, memory, and runtime metrics collected from cloud provider monitoring agents.
                </p>
              </div>
              <span className="text-xs font-mono text-slate-400">{telemetrySnapshots.length} Monitored Snapshots</span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-navy-900/80 border-b border-slate-800 text-slate-400 uppercase font-mono text-[10px] tracking-wider">
                  <tr>
                    <th className="py-3 px-4">Resource ID & Name</th>
                    <th className="py-3 px-4">Type & Region</th>
                    <th className="py-3 px-4">Avg CPU %</th>
                    <th className="py-3 px-4">Peak CPU %</th>
                    <th className="py-3 px-4">Memory %</th>
                    <th className="py-3 px-4">Running Hours</th>
                    <th className="py-3 px-4">Telemetry Health</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/80 text-slate-300">
                  {telemetrySnapshots.length === 0 ? (
                    <tr>
                      <td colSpan="7" className="py-8 text-center text-slate-400">
                        No resource utilization snapshots recorded yet. Connect cloud account or run sync.
                      </td>
                    </tr>
                  ) : (
                    telemetrySnapshots.map((item) => (
                      <tr key={item.id} className="hover:bg-white/[0.02]">
                        <td className="py-3 px-4">
                          <div className="font-bold text-white text-xs">{item.resourceName || item.resourceId}</div>
                          <div className="text-[10px] font-mono text-slate-400">{item.resourceId}</div>
                        </td>
                        <td className="py-3 px-4 font-mono text-[11px]">
                          <div>{item.resourceType}</div>
                          <div className="text-slate-500 text-[10px]">{item.region || 'global'}</div>
                        </td>
                        <td className="py-3 px-4 font-mono">
                          {item.cpuUtilizationAvg != null ? `${item.cpuUtilizationAvg}%` : 'N/A'}
                        </td>
                        <td className="py-3 px-4 font-mono">
                          {item.cpuUtilizationMax != null ? `${item.cpuUtilizationMax}%` : 'N/A'}
                        </td>
                        <td className="py-3 px-4 font-mono">
                          {item.memoryUtilizationAvg != null ? `${item.memoryUtilizationAvg}%` : 'N/A'}
                        </td>
                        <td className="py-3 px-4 font-mono">
                          {item.runningHours || 730} hrs
                        </td>
                        <td className="py-3 px-4">
                          <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${
                            item.telemetryStatus === 'HEALTHY'
                              ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20'
                              : item.telemetryStatus === 'UNDERUTILIZED'
                              ? 'bg-amber-500/10 text-amber-300 border-amber-500/20'
                              : item.telemetryStatus === 'IDLE'
                              ? 'bg-rose-500/10 text-rose-300 border-rose-500/20'
                              : 'bg-slate-800 text-slate-400 border-slate-700'
                          }`}>
                            {item.telemetryStatus}
                          </span>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* DETAIL & AUDIT SLIDE-OVER DRAWER */}
      {selectedRec && (
        <div className="fixed inset-0 z-50 overflow-hidden bg-black/60 backdrop-blur-sm flex justify-end">
          <div className="w-full max-w-xl bg-navy-950 border-l border-white/[0.08] shadow-2xl p-6 flex flex-col justify-between overflow-y-auto animate-slideInRight">
            <div className="space-y-5">
              {/* Drawer Header */}
              <div className="flex items-start justify-between pb-4 border-b border-slate-800">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getProviderBadge(selectedRec.cloudProvider)}`}>
                      {selectedRec.cloudProvider}
                    </span>
                    <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getStatusBadge(selectedRec.status)}`}>
                      {selectedRec.status}
                    </span>
                    <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getPriorityBadge(selectedRec.priority)}`}>
                      {selectedRec.priority}
                    </span>
                  </div>
                  <h3 className="text-lg font-bold text-white pt-1">
                    {selectedRec.resourceName || selectedRec.resourceId}
                  </h3>
                  <div className="text-xs font-mono text-slate-400">
                    ID: {selectedRec.resourceId}
                  </div>
                </div>

                <button
                  onClick={handleCloseDrawer}
                  className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              {/* Financial Impact Cards */}
              <div className="grid grid-cols-2 gap-3">
                <div className="p-3.5 rounded-xl bg-navy-900 border border-slate-800 space-y-1">
                  <span className="text-[10px] font-mono uppercase text-slate-400 block">Est. Monthly Savings</span>
                  <span className="text-xl font-bold font-mono text-emerald-400">
                    +{formatCurrency(selectedRec.estimatedMonthlySavings)}
                  </span>
                  <span className="text-[10px] font-mono text-slate-400 block">
                    ~{formatCurrency(selectedRec.estimatedAnnualSavings)} / yr
                  </span>
                </div>

                <div className="p-3.5 rounded-xl bg-navy-900 border border-slate-800 space-y-1">
                  <span className="text-[10px] font-mono uppercase text-slate-400 block">Cost Comparison</span>
                  <div className="text-xs font-mono">
                    <span className="text-slate-400 line-through">{formatCurrency(selectedRec.currentMonthlyCost)}</span>
                    <span className="text-emerald-400 font-bold ml-1.5">→ {formatCurrency(selectedRec.recommendedMonthlyCost)}</span>
                  </div>
                  <span className="text-[10px] font-mono text-slate-400 block">
                    Net {selectedRec.savingsPercentage || 50}% reduction
                  </span>
                </div>
              </div>

              {/* Resource & Telemetry Context */}
              <div className="p-4 rounded-xl bg-navy-900/60 border border-slate-800 space-y-2">
                <h4 className="text-xs font-bold font-mono text-slate-300 uppercase flex items-center space-x-1.5">
                  <Cpu className="w-3.5 h-3.5 text-blue-400" />
                  <span>Telemetry & Workload Evidence</span>
                </h4>
                {selectedRec.hasTelemetryData ? (
                  <div className="grid grid-cols-2 gap-2 text-xs font-mono pt-1">
                    <div className="p-2 rounded bg-navy-950/80">
                      <span className="text-slate-500 block text-[10px]">Avg CPU</span>
                      <span className="text-white font-bold">{selectedRec.cpuUtilizationAvg}%</span>
                    </div>
                    <div className="p-2 rounded bg-navy-950/80">
                      <span className="text-slate-500 block text-[10px]">Peak CPU</span>
                      <span className="text-white font-bold">{selectedRec.cpuUtilizationMax}%</span>
                    </div>
                  </div>
                ) : (
                  <div className="p-3 rounded-lg bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs flex items-center space-x-2">
                    <AlertTriangle className="w-4 h-4 shrink-0" />
                    <span>Resource identified via configuration & billing metadata. Monitoring agent telemetry not yet linked.</span>
                  </div>
                )}
                <p className="text-xs text-slate-300 leading-relaxed pt-1">
                  {selectedRec.reason}
                </p>
              </div>

              {/* Configuration Comparison */}
              <div className="space-y-1.5">
                <span className="text-[10px] font-mono uppercase text-slate-400 block">Configuration Proposal</span>
                <div className="p-3 rounded-xl bg-navy-900/80 border border-slate-800 text-xs font-mono space-y-1">
                  <div className="text-slate-400">Current: <span className="text-white font-semibold">{selectedRec.currentConfig}</span></div>
                  <div className="text-emerald-400">Suggested: <span className="font-bold">{selectedRec.suggestedConfig}</span></div>
                </div>
              </div>

              {/* Status Transition Action Form */}
              <div className="p-4 rounded-xl bg-navy-900/70 border border-slate-800 space-y-3">
                <h4 className="text-xs font-bold font-mono text-slate-300 uppercase flex items-center space-x-1.5">
                  <Sliders className="w-3.5 h-3.5 text-purple-400" />
                  <span>Update Recommendation Status</span>
                </h4>

                <div className="flex flex-wrap gap-2">
                  {['REVIEWED', 'APPROVED', 'IMPLEMENTED', 'REJECTED'].map((st) => (
                    <button
                      key={st}
                      type="button"
                      onClick={() => handleUpdateStatus(st)}
                      disabled={submittingAction || selectedRec.status === st}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold font-mono transition-all ${
                        selectedRec.status === st
                          ? 'bg-slate-800 text-slate-500 cursor-not-allowed'
                          : st === 'APPROVED'
                          ? 'bg-cyan-600 hover:bg-cyan-500 text-white'
                          : st === 'IMPLEMENTED'
                          ? 'bg-emerald-600 hover:bg-emerald-500 text-white'
                          : st === 'REJECTED'
                          ? 'bg-rose-950/60 hover:bg-rose-900 text-rose-300 border border-rose-500/30'
                          : 'bg-navy-800 hover:bg-navy-700 text-slate-300'
                      }`}
                    >
                      {st}
                    </button>
                  ))}
                </div>

                <div className="space-y-1">
                  <label className="text-[10px] font-mono text-slate-400 block uppercase">Implementation Notes</label>
                  <textarea
                    rows={2}
                    value={actionNotes}
                    onChange={(e) => setActionNotes(e.target.value)}
                    placeholder="Enter context, change-ticket ID, or approval rationale..."
                    className="w-full p-2.5 rounded-xl bg-navy-950 border border-slate-800 text-xs text-white placeholder-slate-500 font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {/* Audit History Trail */}
              <div className="space-y-2">
                <h4 className="text-xs font-bold font-mono text-slate-300 uppercase flex items-center space-x-1.5">
                  <Clock className="w-3.5 h-3.5 text-slate-400" />
                  <span>Lifecycle Audit Trail</span>
                </h4>

                {historyLoading ? (
                  <div className="text-center py-4 text-xs text-slate-500">Loading audit history...</div>
                ) : recHistory.length === 0 ? (
                  <div className="text-xs text-slate-500 italic p-3 rounded-xl bg-navy-900/40">
                    No state transitions recorded yet.
                  </div>
                ) : (
                  <div className="space-y-2 max-h-44 overflow-y-auto pr-1">
                    {recHistory.map((h) => (
                      <div key={h.id} className="p-2.5 rounded-xl bg-navy-900/60 border border-slate-800 text-xs space-y-1">
                        <div className="flex items-center justify-between text-[10px] font-mono">
                          <span className="text-slate-400">
                            {h.previousStatus ? `${h.previousStatus} → ` : ''}
                            <strong className="text-white">{h.newStatus}</strong>
                          </span>
                          <span className="text-slate-500">{formatDate(h.createdAt)}</span>
                        </div>
                        <div className="text-slate-300 text-[11px]">{h.notes}</div>
                        <div className="text-[10px] font-mono text-slate-500">By: {h.actionByUserEmail || 'System'}</div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Drawer Footer */}
            <div className="pt-4 border-t border-slate-800 flex justify-end">
              <button
                onClick={handleCloseDrawer}
                className="px-4 py-2 rounded-xl text-xs font-semibold bg-navy-800 hover:bg-navy-700 text-slate-300"
              >
                Close Drawer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
