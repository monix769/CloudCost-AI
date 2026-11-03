import React, { useState, useEffect } from 'react';
import billingService from '../services/billingService';
import { Search, RefreshCw, ChevronLeft, ChevronRight, CheckCircle2, AlertCircle } from 'lucide-react';

export default function RecentBillingTable({ refreshTrigger }) {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedProvider, setSelectedProvider] = useState('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const fetchRecords = async () => {
    try {
      setLoading(true);
      setError(null);
      const params = {
        page,
        size: 10,
        provider: selectedProvider !== 'ALL' ? selectedProvider : undefined,
        serviceName: searchTerm.trim() ? searchTerm.trim() : undefined,
      };
      const res = await billingService.getBillingRecords(params);
      if (res && res.content) {
        setRecords(res.content);
        setTotalPages(res.totalPages || 1);
        setTotalElements(res.totalElements || 0);
      } else {
        setRecords([]);
        setTotalPages(1);
        setTotalElements(0);
      }
    } catch (err) {
      setError(err.message || 'Failed to load billing records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecords();
  }, [page, selectedProvider, refreshTrigger]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchRecords();
  };

  const getProviderBadge = (provider) => {
    switch (provider) {
      case 'AWS':
        return 'bg-amber-500/15 text-amber-300 border-amber-500/30';
      case 'GCP':
        return 'bg-sky-500/15 text-sky-300 border-sky-500/30';
      case 'AZURE':
        return 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
      {/* Table Header with Search and Provider Filters */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="font-bold text-white text-base">Persisted Cloud Billing Activity</h3>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded font-bold bg-slate-800 text-slate-300 border border-slate-700">
              {totalElements} Live Records
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Real line-item costs stored in MySQL cloudcost_ai.billing_records
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Search Input */}
          <form onSubmit={handleSearchSubmit} className="relative">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search services..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8 pr-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors w-40 sm:w-48"
            />
          </form>

          {/* Provider Filter Tabs */}
          <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono">
            {['ALL', 'AWS', 'GCP', 'AZURE'].map((prov) => (
              <button
                key={prov}
                onClick={() => {
                  setSelectedProvider(prov);
                  setPage(0);
                }}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-all ${
                  selectedProvider === prov
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {prov}
              </button>
            ))}
          </div>

          <button
            onClick={fetchRecords}
            title="Refresh billing data"
            className="p-2 rounded-xl bg-navy-900 border border-slate-800 text-slate-400 hover:text-white"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Error state */}
      {error && (
        <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-800/80 text-slate-400 font-mono text-[11px]">
              <th className="pb-3 font-semibold">Provider / Account</th>
              <th className="pb-3 font-semibold">Service &amp; Usage</th>
              <th className="pb-3 font-semibold">Region</th>
              <th className="pb-3 font-semibold">Billing Period</th>
              <th className="pb-3 font-semibold text-right">Cost (USD)</th>
              <th className="pb-3 font-semibold text-right">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {loading ? (
              [1, 2, 3, 4, 5].map((n) => (
                <tr key={n} className="animate-pulse">
                  <td colSpan="6" className="py-3">
                    <div className="h-5 bg-slate-800/60 rounded w-full"></div>
                  </td>
                </tr>
              ))
            ) : records.length > 0 ? (
              records.map((record) => (
                <tr key={record.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3">
                    <div className="flex items-center space-x-2">
                      <span
                        className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getProviderBadge(
                          record.provider
                        )}`}
                      >
                        {record.provider || 'CLOUD'}
                      </span>
                      <span className="font-medium text-white truncate max-w-[140px] sm:max-w-none">
                        {record.accountName || `Account #${record.cloudAccountId}`}
                      </span>
                    </div>
                  </td>
                  <td className="py-3">
                    <div>
                      <p className="font-semibold text-slate-200">{record.serviceName}</p>
                      <p className="text-[11px] text-slate-400 font-mono">
                        {record.usageType || (record.usageQuantity ? `${record.usageQuantity} ${record.usageUnit || 'units'}` : 'N/A')}
                      </p>
                    </div>
                  </td>
                  <td className="py-3 font-mono text-[11px] text-slate-300">{record.region}</td>
                  <td className="py-3 font-mono text-[11px] text-slate-400">
                    {record.billingPeriodStart} to {record.billingPeriodEnd}
                  </td>
                  <td className="py-3 text-right font-mono font-bold text-white text-sm">
                    ${Number(record.costAmount).toFixed(2)}
                  </td>
                  <td className="py-3 text-right">
                    <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                      <CheckCircle2 className="w-3 h-3" />
                      <span>PERSISTED</span>
                    </span>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" className="py-12 text-center text-slate-400">
                  <p className="font-semibold text-sm">No billing records found in MySQL database.</p>
                  <p className="text-xs text-slate-500 mt-1">Upload a CSV billing export above to populate records.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-3 border-t border-slate-800 text-xs font-mono text-slate-400">
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="p-1.5 rounded-lg bg-navy-900 border border-slate-800 disabled:opacity-40 hover:text-white"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="p-1.5 rounded-lg bg-navy-900 border border-slate-800 disabled:opacity-40 hover:text-white"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
