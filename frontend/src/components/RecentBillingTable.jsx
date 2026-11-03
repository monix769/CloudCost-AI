import React, { useState } from 'react';
import { recentBillingRecords } from '../utils/mockFinOpsData';
import { Search, Filter, ArrowUpRight, FileText, CheckCircle2 } from 'lucide-react';

export default function RecentBillingTable() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedProvider, setSelectedProvider] = useState('ALL');

  const filteredRecords = recentBillingRecords.filter((rec) => {
    const matchesProvider = selectedProvider === 'ALL' || rec.provider === selectedProvider;
    const matchesSearch =
      rec.service.toLowerCase().includes(searchTerm.toLowerCase()) ||
      rec.account.toLowerCase().includes(searchTerm.toLowerCase()) ||
      rec.region.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesProvider && matchesSearch;
  });

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
            <h3 className="font-bold text-white text-base">Recent Cloud Billing Activity</h3>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded font-bold bg-slate-800 text-slate-300 border border-slate-700">
              {filteredRecords.length} Records
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Processed cloud expenditure items ingested across active accounts
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Search Input */}
          <div className="relative">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Filter services or regions..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-8 pr-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors w-44 sm:w-56"
            />
          </div>

          {/* Provider Filter Tabs */}
          <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono">
            {['ALL', 'AWS', 'GCP', 'AZURE'].map((prov) => (
              <button
                key={prov}
                onClick={() => setSelectedProvider(prov)}
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
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-800/80 text-slate-400 font-mono text-[11px]">
              <th className="pb-3 font-semibold">Provider / Account</th>
              <th className="pb-3 font-semibold">Service &amp; Resource</th>
              <th className="pb-3 font-semibold">Region</th>
              <th className="pb-3 font-semibold">Billing Date</th>
              <th className="pb-3 font-semibold text-right">Cost (USD)</th>
              <th className="pb-3 font-semibold text-right">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-sans">
            {filteredRecords.length > 0 ? (
              filteredRecords.map((record) => (
                <tr key={record.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-3">
                    <div className="flex items-center space-x-2">
                      <span
                        className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getProviderBadge(
                          record.provider
                        )}`}
                      >
                        {record.provider}
                      </span>
                      <span className="font-medium text-white truncate max-w-[140px] sm:max-w-none">
                        {record.account}
                      </span>
                    </div>
                  </td>
                  <td className="py-3">
                    <div>
                      <p className="font-semibold text-slate-200">{record.service}</p>
                      <p className="text-[11px] text-slate-400 font-mono">{record.usage}</p>
                    </div>
                  </td>
                  <td className="py-3 font-mono text-[11px] text-slate-300">{record.region}</td>
                  <td className="py-3 font-mono text-[11px] text-slate-400">{record.date}</td>
                  <td className="py-3 text-right font-mono font-bold text-white text-sm">
                    ${record.cost.toFixed(2)}
                  </td>
                  <td className="py-3 text-right">
                    <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                      <CheckCircle2 className="w-3 h-3" />
                      <span>{record.status}</span>
                    </span>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" className="py-12 text-center text-slate-400">
                  <p className="font-semibold text-sm">No billing records match your query.</p>
                  <p className="text-xs text-slate-500 mt-1">Try resetting the search filter or provider tab.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
