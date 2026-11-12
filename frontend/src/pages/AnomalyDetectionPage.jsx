import React, { useState, useEffect, useMemo } from 'react';
import anomalyService from '../services/anomalyService';
import { formatCurrency } from '../utils/formatters';
import { Bar, Doughnut } from 'react-chartjs-2';
import '../utils/chartConfig';
import {
  ShieldAlert,
  AlertTriangle,
  CheckCircle,
  RefreshCw,
  Search,
  Filter,
  Check,
  Loader2,
  AlertCircle,
  Sliders,
  Play,
  Clock,
  ArrowUpRight,
  ChevronLeft,
  ChevronRight,
  X,
  FileText,
  Activity,
  History,
  TrendingUp,
  DollarSign,
  Info,
} from 'lucide-react';

export default function AnomalyDetectionPage() {
  // Primary datasets
  const [anomalies, setAnomalies] = useState([]);
  const [summary, setSummary] = useState(null);
  const [trends, setTrends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters & Pagination
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [selectedProvider, setSelectedProvider] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(0);
  const pageSize = 10;

  // Actions & States
  const [isScanning, setIsScanning] = useState(false);
  const [scanMessage, setScanMessage] = useState(null);
  const [statusUpdatingId, setStatusUpdatingId] = useState(null);

  // Detail Modal
  const [selectedAnomaly, setSelectedAnomaly] = useState(null);
  const [anomalyHistory, setAnomalyHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [transitionNotes, setTransitionNotes] = useState('');
  const [newStatusChoice, setNewStatusChoice] = useState('');

  // Configuration Modal
  const [isConfigOpen, setIsConfigOpen] = useState(false);
  const [configForm, setConfigForm] = useState(null);
  const [configSaving, setConfigSaving] = useState(false);

  // Fetch summary and trends
  const fetchDashboardData = async () => {
    try {
      const [sumData, trendData] = await Promise.all([
        anomalyService.getAnomalySummary(),
        anomalyService.getAnomalyTrends(),
      ]);
      setSummary(sumData);
      setTrends(trendData?.trends || []);
    } catch (err) {
      console.warn('Failed to load anomaly summary or trends:', err.message);
    }
  };

  // Fetch anomalies list
  const fetchAnomalies = async () => {
    try {
      setLoading(true);
      setError(null);
      const params = {
        severity: selectedSeverity !== 'ALL' ? selectedSeverity : undefined,
        status: selectedStatus !== 'ALL' ? selectedStatus : undefined,
        provider: selectedProvider !== 'ALL' ? selectedProvider : undefined,
        serviceName: searchQuery.trim() ? searchQuery.trim() : undefined,
      };
      const data = await anomalyService.getAnomalies(params);
      setAnomalies(Array.isArray(data) ? data : data?.content || []);
      setCurrentPage(0);
    } catch (err) {
      setError(err.message || 'Failed to fetch anomaly records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  useEffect(() => {
    fetchAnomalies();
  }, [selectedSeverity, selectedStatus, selectedProvider, searchQuery]);

  // Handle Manual Detection Trigger
  const handleTriggerDetection = async () => {
    try {
      setIsScanning(true);
      setScanMessage(null);
      const result = await anomalyService.triggerDetection();
      setScanMessage(result?.summaryMessage || 'Anomaly detection scan finished successfully.');
      await Promise.all([fetchAnomalies(), fetchDashboardData()]);
      setTimeout(() => setScanMessage(null), 5000);
    } catch (err) {
      alert('Detection scan failed: ' + err.message);
    } finally {
      setIsScanning(false);
    }
  };

  // Handle Quick Status Update
  const handleQuickStatusUpdate = async (id, newStatus) => {
    try {
      setStatusUpdatingId(id);
      await anomalyService.updateAnomalyStatus(id, newStatus);
      await Promise.all([fetchAnomalies(), fetchDashboardData()]);
      if (selectedAnomaly && selectedAnomaly.id === id) {
        const updated = await anomalyService.getAnomalyById(id);
        setSelectedAnomaly(updated);
        loadHistory(id);
      }
    } catch (err) {
      alert('Status update failed: ' + err.message);
    } finally {
      setStatusUpdatingId(null);
    }
  };

  // Open Detail Modal & Load History
  const openDetailModal = async (anomaly) => {
    setSelectedAnomaly(anomaly);
    setNewStatusChoice(anomaly.status);
    setTransitionNotes('');
    loadHistory(anomaly.id);
  };

  const loadHistory = async (id) => {
    try {
      setHistoryLoading(true);
      const hist = await anomalyService.getAnomalyHistory(id);
      setAnomalyHistory(hist || []);
    } catch (err) {
      console.warn('Failed to load history:', err.message);
    } finally {
      setHistoryLoading(false);
    }
  };

  // Submit Detailed Status Transition
  const handleDetailedStatusTransition = async (e) => {
    e.preventDefault();
    if (!selectedAnomaly || !newStatusChoice) return;
    try {
      setStatusUpdatingId(selectedAnomaly.id);
      const updated = await anomalyService.updateAnomalyStatus(
        selectedAnomaly.id,
        newStatusChoice,
        transitionNotes
      );
      setSelectedAnomaly(updated);
      setTransitionNotes('');
      await Promise.all([fetchAnomalies(), fetchDashboardData(), loadHistory(selectedAnomaly.id)]);
    } catch (err) {
      alert('Error transitioning anomaly status: ' + err.message);
    } finally {
      setStatusUpdatingId(null);
    }
  };

  // Open Configuration Modal
  const openConfigModal = async () => {
    try {
      setIsConfigOpen(true);
      const cfg = await anomalyService.getDetectionConfig();
      setConfigForm(cfg);
    } catch (err) {
      alert('Failed to load configuration: ' + err.message);
    }
  };

  const handleSaveConfig = async (e) => {
    e.preventDefault();
    try {
      setConfigSaving(true);
      const updated = await anomalyService.updateDetectionConfig(configForm);
      setConfigForm(updated);
      alert('Anomaly detection configuration saved successfully.');
      setIsConfigOpen(false);
    } catch (err) {
      alert('Failed to save configuration: ' + err.message);
    } finally {
      setConfigSaving(false);
    }
  };

  // Filtered & Paginated records
  const paginatedAnomalies = useMemo(() => {
    const start = currentPage * pageSize;
    return anomalies.slice(start, start + pageSize);
  }, [anomalies, currentPage]);

  const totalPages = Math.ceil(anomalies.length / pageSize) || 1;

  // Chart 1: Anomaly Trends Data
  const trendsChartData = useMemo(() => {
    const labels = trends.map((t) => t.date);
    const counts = trends.map((t) => t.totalAnomalies);
    const excess = trends.map((t) => Number(t.excessCost || 0));

    return {
      labels,
      datasets: [
        {
          type: 'bar',
          label: 'Anomalies Detected',
          data: counts,
          backgroundColor: 'rgba(59, 130, 246, 0.65)',
          borderColor: '#3b82f6',
          borderWidth: 1,
          borderRadius: 4,
          yAxisID: 'y',
        },
        {
          type: 'line',
          label: 'Excess Cost ($)',
          data: excess,
          borderColor: '#f43f5e',
          backgroundColor: 'rgba(244, 63, 94, 0.15)',
          borderWidth: 2,
          pointRadius: 3,
          fill: true,
          tension: 0.3,
          yAxisID: 'y1',
        },
      ],
    };
  }, [trends]);

  const trendsChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    scales: {
      x: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: '#94a3b8', font: { size: 10 } } },
      y: {
        type: 'linear',
        display: true,
        position: 'left',
        grid: { color: 'rgba(255, 255, 255, 0.05)' },
        ticks: { color: '#94a3b8', precision: 0 },
        title: { display: true, text: 'Count', color: '#94a3b8', font: { size: 10 } },
      },
      y1: {
        type: 'linear',
        display: true,
        position: 'right',
        grid: { drawOnChartArea: false },
        ticks: {
          color: '#f43f5e',
          callback: (val) => '$' + Number(val).toLocaleString(),
        },
        title: { display: true, text: 'Excess ($)', color: '#f43f5e', font: { size: 10 } },
      },
    },
    plugins: {
      legend: { labels: { color: '#cbd5e1', font: { size: 11 } } },
    },
  };

  // Chart 2: Severity Distribution Data
  const severityChartData = useMemo(() => {
    const crit = summary?.criticalCount || 0;
    const high = summary?.highCount || 0;
    const med = summary?.mediumCount || 0;
    const low = summary?.lowCount || 0;

    return {
      labels: ['Critical', 'High', 'Medium', 'Low'],
      datasets: [
        {
          data: [crit, high, med, low],
          backgroundColor: ['#f43f5e', '#f59e0b', '#3b82f6', '#64748b'],
          borderColor: '#0f172a',
          borderWidth: 2,
        },
      ],
    };
  }, [summary]);

  const severityChartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    plugins: {
      legend: { position: 'bottom', labels: { color: '#cbd5e1', font: { size: 11 } } },
    },
  };

  const getSeverityBadge = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-rose-500/20 text-rose-300 border-rose-500/40';
      case 'HIGH':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/40';
      case 'MEDIUM':
        return 'bg-blue-500/20 text-blue-300 border-blue-500/40';
      default:
        return 'bg-slate-800 text-slate-400 border-slate-700';
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'RESOLVED':
        return 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30';
      case 'ACKNOWLEDGED':
        return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
      case 'DISMISSED':
      case 'IGNORED':
        return 'bg-slate-800 text-slate-400 border-slate-700';
      default:
        return 'bg-rose-500/15 text-rose-300 border-rose-500/30 animate-pulse';
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* Header Banner */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center space-x-3">
            <h2 className="text-xl font-extrabold text-white flex items-center space-x-2">
              <span>Real-Time Cost Anomaly Detection Engine</span>
            </h2>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30">
              Milestone 7 Engine
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1 max-w-2xl leading-relaxed">
            Multi-algorithm statistical surveillance engine evaluating actual MySQL billing records. Identifies Z-score outliers,
            moving average deviations, sudden overnight spend spikes, and budget threshold breaches.
          </p>
        </div>

        {/* Global Controls */}
        <div className="flex flex-wrap items-center gap-2.5">
          <button
            onClick={handleTriggerDetection}
            disabled={isScanning}
            className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-semibold text-xs shadow-lg flex items-center space-x-2 transition-all disabled:opacity-50 cursor-pointer"
          >
            {isScanning ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Play className="w-3.5 h-3.5 fill-current" />
            )}
            <span>{isScanning ? 'Scanning...' : 'Run Detection Scan'}</span>
          </button>

          <button
            onClick={openConfigModal}
            className="px-3 py-2 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 font-semibold text-xs flex items-center space-x-1.5 transition-all cursor-pointer"
          >
            <Sliders className="w-3.5 h-3.5" />
            <span>Detection Settings</span>
          </button>

          <button
            onClick={() => {
              fetchAnomalies();
              fetchDashboardData();
            }}
            title="Refresh"
            className="p-2 rounded-xl bg-navy-900 border border-slate-800 text-slate-400 hover:text-white transition-all cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Scan Feedback Alert */}
      {scanMessage && (
        <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center justify-between animate-fadeIn">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>{scanMessage}</span>
          </div>
          <button onClick={() => setScanMessage(null)} className="text-slate-400 hover:text-white">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* KPI Summary Ribbon */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* KPI 1 */}
        <div className="glass-panel rounded-2xl p-4 border border-white/[0.08] shadow-card-elevated space-y-1">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Total Anomalies</span>
            <Activity className="w-4 h-4 text-blue-400" />
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-extrabold text-white font-mono">
              {summary ? summary.totalAnomalies : '—'}
            </span>
            <span className="text-[10px] font-mono text-slate-400">
              ({summary?.openCount || 0} Open)
            </span>
          </div>
          <p className="text-[10px] text-slate-500">Across all providers &amp; workloads</p>
        </div>

        {/* KPI 2 */}
        <div className="glass-panel rounded-2xl p-4 border border-rose-500/20 bg-rose-950/10 shadow-card-elevated space-y-1">
          <div className="flex items-center justify-between text-rose-300 text-xs font-semibold">
            <span>Critical Severity</span>
            <ShieldAlert className="w-4 h-4 text-rose-400 animate-pulse" />
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-extrabold text-rose-400 font-mono">
              {summary ? summary.criticalCount : '—'}
            </span>
            <span className="text-[10px] font-mono text-rose-400/80">Immediate attention</span>
          </div>
          <p className="text-[10px] text-rose-400/60">&gt;200% surge or budget exceeded</p>
        </div>

        {/* KPI 3 */}
        <div className="glass-panel rounded-2xl p-4 border border-amber-500/20 bg-amber-950/10 shadow-card-elevated space-y-1">
          <div className="flex items-center justify-between text-amber-300 text-xs font-semibold">
            <span>High Severity</span>
            <AlertTriangle className="w-4 h-4 text-amber-400" />
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-extrabold text-amber-400 font-mono">
              {summary ? summary.highCount : '—'}
            </span>
            <span className="text-[10px] font-mono text-amber-400/80">&gt;100% surge</span>
          </div>
          <p className="text-[10px] text-amber-400/60">Approaching budget or moving avg</p>
        </div>

        {/* KPI 4 */}
        <div className="glass-panel rounded-2xl p-4 border border-white/[0.08] shadow-card-elevated space-y-1">
          <div className="flex items-center justify-between text-slate-400 text-xs">
            <span>Potential Excess Spending</span>
            <DollarSign className="w-4 h-4 text-rose-400" />
          </div>
          <div className="flex items-baseline space-x-2">
            <span className="text-2xl font-extrabold text-rose-400 font-mono">
              {summary ? formatCurrency(summary.totalExcessCost, summary.currency) : '—'}
            </span>
          </div>
          <p className="text-[10px] text-slate-500">Unbudgeted variance above baselines</p>
        </div>
      </div>

      {/* Visual Analytics Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Chart 1: Anomaly Trends Over Time */}
        <div className="lg:col-span-2 glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div className="flex items-center space-x-2">
              <TrendingUp className="w-4 h-4 text-blue-400" />
              <h3 className="text-xs font-bold text-white uppercase font-mono tracking-wider">
                Anomaly Detection Trends &amp; Excess Cost
              </h3>
            </div>
            <span className="text-[10px] font-mono text-slate-400">Past 30 Days</span>
          </div>
          <div className="h-56">
            {trends.length === 0 ? (
              <div className="h-full flex items-center justify-center text-xs text-slate-500 font-mono">
                No trend history recorded yet
              </div>
            ) : (
              <Bar data={trendsChartData} options={trendsChartOptions} />
            )}
          </div>
        </div>

        {/* Chart 2: Severity Distribution */}
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08] shadow-card-elevated space-y-3">
          <div className="flex items-center justify-between pb-2 border-b border-slate-800">
            <div className="flex items-center space-x-2">
              <ShieldAlert className="w-4 h-4 text-amber-400" />
              <h3 className="text-xs font-bold text-white uppercase font-mono tracking-wider">
                Severity Breakdown
              </h3>
            </div>
            <span className="text-[10px] font-mono text-slate-400">All-Time</span>
          </div>
          <div className="h-56 relative flex items-center justify-center">
            {summary && summary.totalAnomalies > 0 ? (
              <Doughnut data={severityChartData} options={severityChartOptions} />
            ) : (
              <div className="text-xs text-slate-500 font-mono">No anomalies to classify</div>
            )}
          </div>
        </div>
      </div>

      {/* Filter & Search Toolbar */}
      <div className="glass-panel rounded-2xl p-4 border border-white/[0.08] shadow-card-elevated space-y-3">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          {/* Search Box */}
          <div className="relative flex-1 max-w-md">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search by service (e.g. EC2, RDS) or keyword..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono"
            />
          </div>

          {/* Provider Filter */}
          <div className="flex items-center space-x-1 p-1 rounded-xl bg-navy-900 border border-slate-800 text-[11px] font-mono">
            {['ALL', 'AWS', 'AZURE', 'GCP'].map((prov) => (
              <button
                key={prov}
                onClick={() => setSelectedProvider(prov)}
                className={`px-2.5 py-1 rounded-lg font-semibold transition-all cursor-pointer ${
                  selectedProvider === prov
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {prov}
              </button>
            ))}
          </div>

          {/* Severity Filter */}
          <div className="flex items-center space-x-1 p-1 rounded-xl bg-navy-900 border border-slate-800 text-[11px] font-mono">
            {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'].map((sev) => (
              <button
                key={sev}
                onClick={() => setSelectedSeverity(sev)}
                className={`px-2.5 py-1 rounded-lg font-semibold transition-all cursor-pointer ${
                  selectedSeverity === sev
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {sev}
              </button>
            ))}
          </div>

          {/* Status Dropdown */}
          <select
            value={selectedStatus}
            onChange={(e) => setSelectedStatus(e.target.value)}
            className="px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-slate-300 font-mono focus:outline-none focus:border-blue-500"
          >
            <option value="ALL">Status: All</option>
            <option value="OPEN">Status: OPEN</option>
            <option value="ACKNOWLEDGED">Status: ACKNOWLEDGED</option>
            <option value="RESOLVED">Status: RESOLVED</option>
            <option value="DISMISSED">Status: DISMISSED</option>
          </select>
        </div>
      </div>

      {/* Main Anomalies Table & Card View */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-3">
          {[1, 2, 3].map((n) => (
            <div key={n} className="glass-panel rounded-2xl p-5 border border-white/[0.08] animate-pulse space-y-3">
              <div className="h-5 w-1/4 bg-slate-800 rounded"></div>
              <div className="h-12 bg-slate-800/60 rounded"></div>
            </div>
          ))}
        </div>
      ) : anomalies.length === 0 ? (
        <div className="glass-panel rounded-2xl p-12 border border-white/[0.08] text-center space-y-3">
          <CheckCircle className="w-12 h-12 text-emerald-500 mx-auto" />
          <h3 className="text-base font-semibold text-white">Zero Spending Anomalies Detected</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            All multi-cloud workloads are operating within statistical variance thresholds and historical moving average baselines.
          </p>
          <button
            onClick={handleTriggerDetection}
            className="px-4 py-2 rounded-xl bg-navy-900 hover:bg-navy-800 text-blue-400 border border-blue-500/30 text-xs font-semibold inline-flex items-center space-x-2"
          >
            <Play className="w-3.5 h-3.5" />
            <span>Run Manual Surveillance Scan</span>
          </button>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 font-mono text-[10px] uppercase tracking-wider bg-navy-950/40">
                  <th className="py-3 px-4">Severity</th>
                  <th className="py-3 px-4">Workload / Service</th>
                  <th className="py-3 px-4">Detection Algorithm</th>
                  <th className="py-3 px-4">Expected vs Actual</th>
                  <th className="py-3 px-4">Excess Spend</th>
                  <th className="py-3 px-4">Deviation</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {paginatedAnomalies.map((item) => (
                  <tr
                    key={item.id}
                    className="hover:bg-navy-900/40 transition-colors group cursor-pointer"
                    onClick={() => openDetailModal(item)}
                  >
                    {/* Severity */}
                    <td className="py-3.5 px-4">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded border ${getSeverityBadge(item.severity)}`}>
                        {item.severity}
                      </span>
                    </td>

                    {/* Workload / Service */}
                    <td className="py-3.5 px-4 font-sans">
                      <div className="font-bold text-white text-sm flex items-center space-x-2">
                        <span>{item.serviceName}</span>
                        <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-slate-800 text-slate-300 border border-slate-700">
                          {item.provider || 'CLOUD'}
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-400 font-mono mt-0.5">
                        {item.accountName || 'Cloud Account'} • {item.region || 'GLOBAL'}
                      </div>
                    </td>

                    {/* Detection Method */}
                    <td className="py-3.5 px-4">
                      <span className="px-2 py-0.5 rounded text-[10px] bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 font-bold">
                        {item.detectionMethod || 'Z_SCORE'}
                      </span>
                    </td>

                    {/* Expected vs Actual */}
                    <td className="py-3.5 px-4">
                      <div className="text-slate-400 text-[11px]">
                        Exp: ${Number(item.expectedCost || 0).toFixed(2)}
                      </div>
                      <div className="text-white font-bold text-xs">
                        Act: ${Number(item.actualCost || 0).toFixed(2)}
                      </div>
                    </td>

                    {/* Excess Spend */}
                    <td className="py-3.5 px-4">
                      <span className="text-rose-400 font-bold text-xs">
                        +${Number(item.excessCost || 0).toFixed(2)}
                      </span>
                    </td>

                    {/* Deviation */}
                    <td className="py-3.5 px-4">
                      <span className="text-amber-400 font-bold text-xs">
                        +{Number(item.deviationPercentage || 0).toFixed(1)}%
                      </span>
                    </td>

                    {/* Status */}
                    <td className="py-3.5 px-4">
                      <span className={`text-[10px] font-bold px-2 py-0.5 rounded border ${getStatusBadge(item.status)}`}>
                        {item.status}
                      </span>
                    </td>

                    {/* Actions */}
                    <td
                      className="py-3.5 px-4 text-right"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div className="flex items-center justify-end space-x-1.5 text-xs">
                        {item.status === 'OPEN' && (
                          <button
                            onClick={() => handleQuickStatusUpdate(item.id, 'ACKNOWLEDGED')}
                            disabled={statusUpdatingId === item.id}
                            title="Acknowledge"
                            className="px-2 py-1 rounded bg-navy-900 hover:bg-navy-800 text-blue-400 border border-blue-500/30 font-semibold text-[11px]"
                          >
                            Ack
                          </button>
                        )}
                        {item.status !== 'RESOLVED' && (
                          <button
                            onClick={() => handleQuickStatusUpdate(item.id, 'RESOLVED')}
                            disabled={statusUpdatingId === item.id}
                            title="Mark Resolved"
                            className="px-2 py-1 rounded bg-emerald-600/20 hover:bg-emerald-600/30 text-emerald-300 border border-emerald-500/40 font-semibold text-[11px]"
                          >
                            Resolve
                          </button>
                        )}
                        <button
                          onClick={() => openDetailModal(item)}
                          className="px-2.5 py-1 rounded bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 text-[11px] font-semibold"
                        >
                          Inspect →
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          <div className="flex items-center justify-between pt-3 border-t border-slate-800 text-xs font-mono">
            <span className="text-slate-400">
              Showing {currentPage * pageSize + 1}–{Math.min((currentPage + 1) * pageSize, anomalies.length)} of {anomalies.length} records
            </span>
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                disabled={currentPage === 0}
                className="px-2.5 py-1 rounded bg-navy-900 border border-slate-800 text-slate-300 disabled:opacity-40"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
              </button>
              <span className="text-slate-300">
                Page {currentPage + 1} of {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={currentPage >= totalPages - 1}
                className="px-2.5 py-1 rounded bg-navy-900 border border-slate-800 text-slate-300 disabled:opacity-40"
              >
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      )}

      {/* =====================================================================
          DETAIL INSPECTION & STATUS HISTORY MODAL
          ===================================================================== */}
      {selectedAnomaly && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-navy-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel rounded-2xl border border-white/[0.1] shadow-2xl max-w-2xl w-full p-6 space-y-5 max-h-[90vh] overflow-y-auto">
            {/* Header */}
            <div className="flex items-start justify-between pb-3 border-b border-slate-800">
              <div>
                <div className="flex items-center space-x-2">
                  <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getSeverityBadge(selectedAnomaly.severity)}`}>
                    {selectedAnomaly.severity}
                  </span>
                  <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getStatusBadge(selectedAnomaly.status)}`}>
                    {selectedAnomaly.status}
                  </span>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 font-bold">
                    {selectedAnomaly.detectionMethod || 'Z_SCORE'}
                  </span>
                </div>
                <h3 className="text-lg font-bold text-white mt-1.5">
                  {selectedAnomaly.serviceName}
                </h3>
                <p className="text-xs text-slate-400 font-mono">
                  {selectedAnomaly.accountName} • {selectedAnomaly.provider} • Region: {selectedAnomaly.region || 'GLOBAL'}
                </p>
              </div>

              <button
                onClick={() => setSelectedAnomaly(null)}
                className="p-1.5 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Metrics Breakdown Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-3.5 rounded-xl bg-navy-900/70 border border-slate-800 text-xs font-mono">
              <div>
                <span className="text-[10px] text-slate-400 block">Expected Cost</span>
                <span className="text-sm font-bold text-slate-200">
                  ${Number(selectedAnomaly.expectedCost || 0).toFixed(2)}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block">Actual Spend</span>
                <span className="text-sm font-bold text-rose-400">
                  ${Number(selectedAnomaly.actualCost || 0).toFixed(2)}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block">Excess Cost</span>
                <span className="text-sm font-bold text-rose-400">
                  +${Number(selectedAnomaly.excessCost || 0).toFixed(2)}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 block">Deviation</span>
                <span className="text-sm font-bold text-amber-400">
                  +{Number(selectedAnomaly.deviationPercentage || 0).toFixed(1)}%
                </span>
              </div>
            </div>

            {/* Root Cause & Recommendation */}
            <div className="space-y-3 text-xs">
              <div className="space-y-1">
                <span className="text-[10px] font-mono uppercase font-bold text-slate-400 tracking-wider flex items-center space-x-1">
                  <FileText className="w-3 h-3 text-blue-400" />
                  <span>FinOps Root Cause Diagnostic:</span>
                </span>
                <p className="text-slate-300 leading-relaxed bg-navy-950/80 p-3 rounded-xl border border-white/[0.04] font-sans">
                  {selectedAnomaly.rootCauseSummary || 'Identified statistical deviation exceeding baseline parameters.'}
                </p>
              </div>

              {selectedAnomaly.recommendedAction && (
                <div className="space-y-1">
                  <span className="text-[10px] font-mono uppercase font-bold text-emerald-400 tracking-wider flex items-center space-x-1">
                    <CheckCircle className="w-3 h-3 text-emerald-400" />
                    <span>Recommended Corrective Action:</span>
                  </span>
                  <p className="text-slate-300 leading-relaxed bg-emerald-950/20 p-3 rounded-xl border border-emerald-500/20 font-sans">
                    {selectedAnomaly.recommendedAction}
                  </p>
                </div>
              )}
            </div>

            {/* Status Transition History Audit Log */}
            <div className="space-y-2 text-xs">
              <span className="text-[10px] font-mono uppercase font-bold text-slate-400 tracking-wider flex items-center space-x-1">
                <History className="w-3 h-3 text-indigo-400" />
                <span>Lifecycle Transition Audit History:</span>
              </span>

              {historyLoading ? (
                <div className="p-3 text-center text-xs text-slate-500 font-mono">
                  Loading transition history...
                </div>
              ) : anomalyHistory.length === 0 ? (
                <div className="p-3 rounded-xl bg-navy-950/50 border border-slate-800 text-slate-500 font-mono text-[11px]">
                  Initial detection state: OPEN. No status changes recorded yet.
                </div>
              ) : (
                <div className="space-y-1.5 max-h-40 overflow-y-auto">
                  {anomalyHistory.map((h) => (
                    <div
                      key={h.id}
                      className="p-2.5 rounded-xl bg-navy-950/60 border border-slate-800 text-[11px] font-mono flex items-center justify-between"
                    >
                      <div>
                        <div className="text-white font-semibold flex items-center space-x-1.5">
                          <span className="text-slate-400">{h.previousStatus || 'INIT'}</span>
                          <span>→</span>
                          <span className="text-blue-400">{h.newStatus}</span>
                          <span className="text-slate-500 text-[10px]">({h.actionByUserEmail || 'OPERATOR'})</span>
                        </div>
                        {h.notes && <p className="text-slate-300 font-sans text-xs mt-0.5">{h.notes}</p>}
                      </div>
                      <span className="text-[10px] text-slate-500 shrink-0">
                        {new Date(h.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Status Transition Form */}
            <form onSubmit={handleDetailedStatusTransition} className="pt-3 border-t border-slate-800 space-y-3">
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
                <select
                  value={newStatusChoice}
                  onChange={(e) => setNewStatusChoice(e.target.value)}
                  className="px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-700 text-xs text-white font-mono"
                >
                  <option value="OPEN">Set: OPEN</option>
                  <option value="ACKNOWLEDGED">Set: ACKNOWLEDGED</option>
                  <option value="RESOLVED">Set: RESOLVED</option>
                  <option value="DISMISSED">Set: DISMISSED</option>
                </select>

                <input
                  type="text"
                  placeholder="Optional resolution notes or investigation findings..."
                  value={transitionNotes}
                  onChange={(e) => setTransitionNotes(e.target.value)}
                  className="flex-1 px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-700 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500"
                />

                <button
                  type="submit"
                  disabled={statusUpdatingId === selectedAnomaly.id}
                  className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs flex items-center justify-center space-x-1 shadow-md"
                >
                  {statusUpdatingId === selectedAnomaly.id ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    <Check className="w-3.5 h-3.5" />
                  )}
                  <span>Apply Transition</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =====================================================================
          DETECTION CONFIGURATION MODAL / DRAWER
          ===================================================================== */}
      {isConfigOpen && configForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-navy-950/80 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel rounded-2xl border border-white/[0.1] shadow-2xl max-w-lg w-full p-6 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <Sliders className="w-4 h-4 text-blue-400" />
                <h3 className="text-base font-bold text-white">Detection Parameters</h3>
              </div>
              <button
                onClick={() => setIsConfigOpen(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSaveConfig} className="space-y-4 text-xs font-mono">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">
                  Z-Score Threshold (σ standard deviations):
                </label>
                <input
                  type="number"
                  step="0.1"
                  min="1.0"
                  max="5.0"
                  value={configForm.zScoreThreshold || 2.5}
                  onChange={(e) =>
                    setConfigForm({ ...configForm, zScoreThreshold: parseFloat(e.target.value) })
                  }
                  className="w-full px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-700 text-white"
                />
                <span className="text-[10px] text-slate-500">Default: 2.50 (98.7% statistical confidence)</span>
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">
                  Moving Average Window (Days):
                </label>
                <input
                  type="number"
                  min="3"
                  max="90"
                  value={configForm.movingAvgWindowDays || 14}
                  onChange={(e) =>
                    setConfigForm({ ...configForm, movingAvgWindowDays: parseInt(e.target.value, 10) })
                  }
                  className="w-full px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-700 text-white"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">
                  Moving Average Deviation Threshold (%):
                </label>
                <input
                  type="number"
                  step="1"
                  min="5"
                  max="500"
                  value={configForm.movingAvgDeviationPercent || 30.0}
                  onChange={(e) =>
                    setConfigForm({ ...configForm, movingAvgDeviationPercent: parseFloat(e.target.value) })
                  }
                  className="w-full px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-700 text-white"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">
                  Daily Spending Spike Surge (%):
                </label>
                <input
                  type="number"
                  step="5"
                  min="10"
                  max="1000"
                  value={configForm.spendingSpikePercent || 50.0}
                  onChange={(e) =>
                    setConfigForm({ ...configForm, spendingSpikePercent: parseFloat(e.target.value) })
                  }
                  className="w-full px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-700 text-white"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">
                  Minimum Spike Dollar Amount ($):
                </label>
                <input
                  type="number"
                  step="5"
                  min="0"
                  value={configForm.spendingSpikeMinAmount || 50.0}
                  onChange={(e) =>
                    setConfigForm({ ...configForm, spendingSpikeMinAmount: parseFloat(e.target.value) })
                  }
                  className="w-full px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-700 text-white"
                />
              </div>

              <div className="flex items-center space-x-2 pt-2">
                <input
                  type="checkbox"
                  id="autoDetection"
                  checked={Boolean(configForm.isAutomatedDetectionEnabled)}
                  onChange={(e) =>
                    setConfigForm({ ...configForm, isAutomatedDetectionEnabled: e.target.checked })
                  }
                  className="w-4 h-4 rounded border-slate-700 text-blue-600 focus:ring-0"
                />
                <label htmlFor="autoDetection" className="text-slate-300 font-semibold cursor-pointer">
                  Enable background automated scheduled detection sweeps
                </label>
              </div>

              <div className="pt-3 border-t border-slate-800 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setIsConfigOpen(false)}
                  className="px-3 py-1.5 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-400 font-semibold text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={configSaving}
                  className="px-4 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs shadow-md flex items-center space-x-1"
                >
                  {configSaving && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Save Configuration</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
