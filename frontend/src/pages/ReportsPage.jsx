import React, { useState, useEffect, useCallback } from 'react';
import {
  FileText,
  Download,
  Trash2,
  Calendar,
  Filter,
  Plus,
  RefreshCw,
  AlertCircle,
  CheckCircle,
  Clock,
  FileSpreadsheet,
  Sparkles,
  X,
  ChevronDown,
  Layers,
  TrendingDown,
  AlertTriangle,
  Building2,
} from 'lucide-react';
import reportService from '../services/reportService';
import { useAuth } from '../context/AuthContext';

export default function ReportsPage() {
  const { user } = useAuth();
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [statusMessage, setStatusMessage] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  // Filter state
  const [typeFilter, setTypeFilter] = useState('');

  // Modal state
  const [showModal, setShowModal] = useState(false);
  const [formData, setFormData] = useState({
    reportType: 'EXECUTIVE_SUMMARY',
    format: 'PDF',
    startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    endDate: new Date().toISOString().split('T')[0],
    provider: '',
    currency: 'USD',
  });

  const reportTypes = [
    { value: 'EXECUTIVE_SUMMARY', label: 'Executive FinOps Summary', desc: 'Holistic KPI breakdown, monthly variances, and strategic recommendations', icon: Sparkles },
    { value: 'CLOUD_SPENDING', label: 'Cloud Spending Report', desc: 'Granular breakdown of cloud expenses by service, provider, and account', icon: Layers },
    { value: 'MONTHLY_COST_ANALYSIS', label: 'Monthly Cost Analysis', desc: 'Month-over-month trajectory, growth vectors, and variance commentary', icon: Calendar },
    { value: 'PROVIDER_COMPARISON', label: 'Provider Comparison Report', desc: 'Multi-cloud spending distribution across AWS, Azure, and GCP', icon: Building2 },
    { value: 'BUDGET_UTILIZATION', label: 'Budget Utilization Report', desc: 'Budget thresholds, burn rates, and financial overage alerts', icon: Clock },
    { value: 'ANOMALY_DETECTION', label: 'Anomaly Detection Report', desc: 'Unusual spending spikes, Z-score flags, and resolution audit trail', icon: AlertTriangle },
    { value: 'COST_OPTIMIZATION', label: 'Cost Optimization Report', desc: 'Idle resources, rightsizing candidates, and actionable savings opportunities', icon: TrendingDown },
    { value: 'SAVINGS_SUMMARY', label: 'Potential & Realized Savings', desc: 'Implemented FinOps optimizations vs pipeline savings forecast', icon: CheckCircle },
  ];

  const fetchReports = useCallback(async () => {
    try {
      setLoading(true);
      const params = {};
      if (typeFilter) params.type = typeFilter;
      const data = await reportService.getReports(params);
      setReports(Array.isArray(data) ? data : []);
      setErrorMessage(null);
    } catch (err) {
      console.error('Failed to fetch reports:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to load report history');
    } finally {
      setLoading(false);
    }
  }, [typeFilter]);

  useEffect(() => {
    fetchReports();
  }, [fetchReports]);

  const handleGenerate = async (e) => {
    e.preventDefault();
    try {
      setGenerating(true);
      setErrorMessage(null);
      const payload = {
        reportType: formData.reportType,
        format: formData.format,
        startDate: formData.startDate || null,
        endDate: formData.endDate || null,
        provider: formData.provider || null,
        currency: formData.currency || 'USD',
      };
      const created = await reportService.generateReport(payload);
      setStatusMessage(`Report "${created.reportName}" generated successfully.`);
      setShowModal(false);
      fetchReports();
      setTimeout(() => setStatusMessage(null), 4000);
    } catch (err) {
      console.error('Failed to generate report:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to generate report');
    } finally {
      setGenerating(false);
    }
  };

  const handleQuickGenerate = async (type, format) => {
    try {
      setGenerating(true);
      setErrorMessage(null);
      const payload = {
        reportType: type,
        format: format,
        startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
        endDate: new Date().toISOString().split('T')[0],
        currency: 'USD',
      };
      const created = await reportService.generateReport(payload);
      setStatusMessage(`Report "${created.reportName}" generated successfully.`);
      fetchReports();
      setTimeout(() => setStatusMessage(null), 4000);
    } catch (err) {
      console.error('Quick generate failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to generate report');
    } finally {
      setGenerating(false);
    }
  };

  const handleDownload = async (report) => {
    try {
      setDownloadingId(report.id);
      await reportService.downloadReport(report.id, report.fileName);
      setStatusMessage(`Downloaded ${report.fileName}`);
      // Refresh count
      fetchReports();
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error('Download failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to download report file');
    } finally {
      setDownloadingId(null);
    }
  };

  const handleDelete = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete report "${name}"? This file will be permanently removed.`)) {
      return;
    }
    try {
      setDeletingId(id);
      await reportService.deleteReport(id);
      setStatusMessage(`Report deleted.`);
      fetchReports();
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error('Delete failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to delete report');
    } finally {
      setDeletingId(null);
    }
  };

  const formatFileSize = (bytes) => {
    if (!bytes || bytes === 0) return '0 B';
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(2) + ' MB';
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Alert Notifications */}
      {statusMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>{statusMessage}</span>
          </div>
          <button onClick={() => setStatusMessage(null)} className="hover:text-emerald-200">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="hover:text-rose-200">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <FileText className="w-5 h-5 text-blue-400" />
            <span>Executive FinOps Reports &amp; Statements</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Generate and export boardroom-ready PDF statements and RFC-4180 CSV exports powered by live FinOps telemetry
          </p>
        </div>

        <div className="flex items-center space-x-3 self-start sm:self-auto">
          <button
            onClick={() => fetchReports()}
            disabled={loading}
            className="p-2 rounded-xl border border-slate-800 bg-navy-900 hover:bg-navy-800 text-slate-300 transition-colors"
            title="Refresh history"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={() => setShowModal(true)}
            className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all"
          >
            <Plus className="w-4 h-4" />
            <span>Generate New Report</span>
          </button>
        </div>
      </div>

      {/* Quick Launch Cards */}
      <div>
        <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Instant FinOps Export Templates</h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="glass-panel p-4 rounded-xl border border-white/[0.08] hover:border-blue-500/40 transition-all flex flex-col justify-between space-y-3">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-500/10 text-rose-300 border border-rose-500/20 font-bold">PDF</span>
                <Sparkles className="w-4 h-4 text-amber-400" />
              </div>
              <h4 className="font-bold text-white text-sm">Executive Summary</h4>
              <p className="text-[11px] text-slate-400 mt-1">Boardroom KPI brief with variances, trends, and top cost drivers.</p>
            </div>
            <button
              onClick={() => handleQuickGenerate('EXECUTIVE_SUMMARY', 'PDF')}
              disabled={generating}
              className="w-full py-1.5 px-3 rounded-lg bg-navy-800 hover:bg-navy-700 text-blue-400 text-xs font-semibold border border-blue-500/20 transition-all flex items-center justify-center space-x-1"
            >
              <span>Quick Export</span>
            </button>
          </div>

          <div className="glass-panel p-4 rounded-xl border border-white/[0.08] hover:border-emerald-500/40 transition-all flex flex-col justify-between space-y-3">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 font-bold">CSV</span>
                <FileSpreadsheet className="w-4 h-4 text-emerald-400" />
              </div>
              <h4 className="font-bold text-white text-sm">Cloud Spending Export</h4>
              <p className="text-[11px] text-slate-400 mt-1">Raw telemetry breakdown for custom spreadsheet modeling and audit.</p>
            </div>
            <button
              onClick={() => handleQuickGenerate('CLOUD_SPENDING', 'CSV')}
              disabled={generating}
              className="w-full py-1.5 px-3 rounded-lg bg-navy-800 hover:bg-navy-700 text-emerald-400 text-xs font-semibold border border-emerald-500/20 transition-all flex items-center justify-center space-x-1"
            >
              <span>Quick Export</span>
            </button>
          </div>

          <div className="glass-panel p-4 rounded-xl border border-white/[0.08] hover:border-blue-500/40 transition-all flex flex-col justify-between space-y-3">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-500/10 text-rose-300 border border-rose-500/20 font-bold">PDF</span>
                <TrendingDown className="w-4 h-4 text-blue-400" />
              </div>
              <h4 className="font-bold text-white text-sm">Optimization Brief</h4>
              <p className="text-[11px] text-slate-400 mt-1">Actionable idle resources and rightsizing recommendations.</p>
            </div>
            <button
              onClick={() => handleQuickGenerate('COST_OPTIMIZATION', 'PDF')}
              disabled={generating}
              className="w-full py-1.5 px-3 rounded-lg bg-navy-800 hover:bg-navy-700 text-blue-400 text-xs font-semibold border border-blue-500/20 transition-all flex items-center justify-center space-x-1"
            >
              <span>Quick Export</span>
            </button>
          </div>

          <div className="glass-panel p-4 rounded-xl border border-white/[0.08] hover:border-purple-500/40 transition-all flex flex-col justify-between space-y-3">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-500/10 text-rose-300 border border-rose-500/20 font-bold">PDF</span>
                <Building2 className="w-4 h-4 text-purple-400" />
              </div>
              <h4 className="font-bold text-white text-sm">Multi-Cloud Comparison</h4>
              <p className="text-[11px] text-slate-400 mt-1">Cross-provider share, cost efficiency index, and workload alignment.</p>
            </div>
            <button
              onClick={() => handleQuickGenerate('PROVIDER_COMPARISON', 'PDF')}
              disabled={generating}
              className="w-full py-1.5 px-3 rounded-lg bg-navy-800 hover:bg-navy-700 text-purple-400 text-xs font-semibold border border-purple-500/20 transition-all flex items-center justify-center space-x-1"
            >
              <span>Quick Export</span>
            </button>
          </div>
        </div>
      </div>

      {/* History & Filter Bar */}
      <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
          <div>
            <h3 className="font-bold text-white text-sm flex items-center space-x-2">
              <Calendar className="w-4 h-4 text-blue-400" />
              <span>Generated Reports Archive</span>
              <span className="text-xs font-mono font-normal text-slate-400">({reports.length})</span>
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">Secure repository of generated enterprise statements and audits</p>
          </div>

          <div className="flex items-center space-x-3">
            <Filter className="w-4 h-4 text-slate-400 shrink-0" />
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-blue-500 font-mono"
            >
              <option value="">All Report Types</option>
              {reportTypes.map((t) => (
                <option key={t.value} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* History Table */}
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center space-y-3">
            <RefreshCw className="w-6 h-6 text-blue-400 animate-spin" />
            <span className="text-xs text-slate-400">Loading enterprise reports...</span>
          </div>
        ) : reports.length === 0 ? (
          <div className="py-12 text-center space-y-3">
            <FileText className="w-10 h-10 text-slate-600 mx-auto" />
            <h4 className="text-sm font-semibold text-slate-300">No reports generated yet</h4>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Select a quick export template above or generate a custom PDF / CSV statement for your organization.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800/80 text-slate-400 font-mono uppercase text-[11px]">
                  <th className="py-3 px-4">Report Name</th>
                  <th className="py-3 px-3">Type</th>
                  <th className="py-3 px-3">Format</th>
                  <th className="py-3 px-3">Period</th>
                  <th className="py-3 px-3">Generated</th>
                  <th className="py-3 px-3">Size</th>
                  <th className="py-3 px-3">Downloads</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/40">
                {reports.map((r) => (
                  <tr key={r.id} className="hover:bg-white/[0.02] transition-colors">
                    <td className="py-3 px-4">
                      <div className="font-semibold text-white truncate max-w-xs">{r.reportName}</div>
                      <div className="text-[11px] font-mono text-slate-400 truncate max-w-xs">{r.fileName}</div>
                    </td>
                    <td className="py-3 px-3">
                      <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 border border-blue-500/20">
                        {r.reportType}
                      </span>
                    </td>
                    <td className="py-3 px-3">
                      {r.format === 'PDF' ? (
                        <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-rose-500/10 text-rose-300 border border-rose-500/20">
                          PDF
                        </span>
                      ) : (
                        <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                          CSV
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-3 font-mono text-slate-300 whitespace-nowrap">
                      {r.startDate && r.endDate ? `${r.startDate} → ${r.endDate}` : 'All-Time'}
                    </td>
                    <td className="py-3 px-3 text-slate-400 whitespace-nowrap">
                      <div>{new Date(r.createdAt).toLocaleDateString()}</div>
                      <div className="text-[10px] text-slate-500">{r.generatedByName || 'System'}</div>
                    </td>
                    <td className="py-3 px-3 font-mono text-slate-400 whitespace-nowrap">
                      {formatFileSize(r.fileSizeBytes)}
                    </td>
                    <td className="py-3 px-3 font-mono text-slate-300 text-center">
                      {r.downloadCount || 0}
                    </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end space-x-2">
                        <button
                          onClick={() => handleDownload(r)}
                          disabled={downloadingId === r.id}
                          className="inline-flex items-center space-x-1 px-2.5 py-1 rounded-lg bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 text-xs font-semibold transition-colors"
                          title="Download report file"
                        >
                          <Download className={`w-3.5 h-3.5 ${downloadingId === r.id ? 'animate-bounce' : ''}`} />
                          <span>Download</span>
                        </button>
                        <button
                          onClick={() => handleDelete(r.id, r.reportName)}
                          disabled={deletingId === r.id}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
                          title="Delete report"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
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

      {/* Generation Wizard Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel w-full max-w-lg rounded-2xl border border-white/[0.12] p-6 shadow-2xl space-y-5 bg-navy-950">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <Sparkles className="w-5 h-5 text-blue-400" />
                <h3 className="font-bold text-white text-base">Generate Enterprise Report</h3>
              </div>
              <button
                onClick={() => setShowModal(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-white/10"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleGenerate} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Report Category</label>
                <select
                  value={formData.reportType}
                  onChange={(e) => setFormData({ ...formData, reportType: e.target.value })}
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-medium focus:outline-none focus:border-blue-500"
                >
                  {reportTypes.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
                <p className="text-[11px] text-slate-400 mt-1">
                  {reportTypes.find((t) => t.value === formData.reportType)?.desc}
                </p>
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Export File Format</label>
                <div className="grid grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, format: 'PDF' })}
                    className={`py-2 px-3 rounded-xl border text-center font-bold transition-all flex items-center justify-center space-x-2 ${
                      formData.format === 'PDF'
                        ? 'bg-rose-500/20 border-rose-500 text-rose-300'
                        : 'bg-navy-900 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <FileText className="w-4 h-4" />
                    <span>PDF Statement</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setFormData({ ...formData, format: 'CSV' })}
                    className={`py-2 px-3 rounded-xl border text-center font-bold transition-all flex items-center justify-center space-x-2 ${
                      formData.format === 'CSV'
                        ? 'bg-emerald-500/20 border-emerald-500 text-emerald-300'
                        : 'bg-navy-900 border-slate-800 text-slate-400 hover:text-white'
                    }`}
                  >
                    <FileSpreadsheet className="w-4 h-4" />
                    <span>RFC-4180 CSV</span>
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">Start Date</label>
                  <input
                    type="date"
                    value={formData.startDate}
                    onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                    className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">End Date</label>
                  <input
                    type="date"
                    value={formData.endDate}
                    onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                    className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">Cloud Provider</label>
                  <select
                    value={formData.provider}
                    onChange={(e) => setFormData({ ...formData, provider: e.target.value })}
                    className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                  >
                    <option value="">All Connected Providers</option>
                    <option value="AWS">Amazon Web Services (AWS)</option>
                    <option value="AZURE">Microsoft Azure</option>
                    <option value="GCP">Google Cloud Platform (GCP)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-slate-300 font-semibold mb-1">Reporting Currency</label>
                  <select
                    value={formData.currency}
                    onChange={(e) => setFormData({ ...formData, currency: e.target.value })}
                    className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                  >
                    <option value="USD">USD ($)</option>
                    <option value="EUR">EUR (€)</option>
                    <option value="GBP">GBP (£)</option>
                    <option value="INR">INR (₹)</option>
                    <option value="CAD">CAD (C$)</option>
                  </select>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-800 flex items-center justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 transition-colors font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={generating}
                  className="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-lg shadow-blue-500/20 transition-all flex items-center space-x-2"
                >
                  {generating ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Compiling Telemetry...</span>
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" />
                      <span>Generate Statement</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
