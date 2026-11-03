import React, { useState } from 'react';
import { PlusCircle, Bell, Search, Download, Check, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function QuickActions() {
  const [scanning, setScanning] = useState(false);
  const [scanResult, setScanResult] = useState(null);

  const handleRunScan = () => {
    setScanning(true);
    setScanResult(null);
    setTimeout(() => {
      setScanning(false);
      setScanResult('Scan complete: 0 new anomalies detected across 3 accounts.');
      setTimeout(() => setScanResult(null), 5000);
    }, 1500);
  };

  return (
    <div className="rounded-2xl border border-white/[0.08] bg-navy-800/50 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
      <div className="flex items-center space-x-3">
        <div className="p-2.5 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400">
          <Sparkles className="w-5 h-5" />
        </div>
        <div>
          <h4 className="font-bold text-white text-sm">FinOps Fast Actions</h4>
          <p className="text-xs text-slate-400">
            Rapid shortcuts for cloud account provisioning, budget guardrails, and reporting
          </p>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Link
          to="/cloud-accounts"
          className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-sm transition-all"
        >
          <PlusCircle className="w-3.5 h-3.5" />
          <span>Link Cloud Account</span>
        </Link>

        <Link
          to="/budgets"
          className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-navy-900 hover:bg-navy-750 text-slate-200 border border-slate-700 transition-all"
        >
          <Bell className="w-3.5 h-3.5 text-amber-400" />
          <span>New Budget Alert</span>
        </Link>

        <button
          onClick={handleRunScan}
          disabled={scanning}
          className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-navy-900 hover:bg-navy-750 text-slate-200 border border-slate-700 transition-all disabled:opacity-50"
        >
          <Search className={`w-3.5 h-3.5 text-blue-400 ${scanning ? 'animate-spin' : ''}`} />
          <span>{scanning ? 'Auditing Cloud Fleet...' : 'Run Anomaly Scan'}</span>
        </button>

        <Link
          to="/reports"
          className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-navy-900 hover:bg-navy-750 text-slate-200 border border-slate-700 transition-all"
        >
          <Download className="w-3.5 h-3.5 text-emerald-400" />
          <span>Export FinOps Report</span>
        </Link>
      </div>

      {scanResult && (
        <div className="w-full text-xs font-mono text-emerald-400 bg-emerald-950/40 border border-emerald-500/30 p-2 rounded-lg flex items-center space-x-2">
          <Check className="w-4 h-4 shrink-0" />
          <span>{scanResult}</span>
        </div>
      )}
    </div>
  );
}
