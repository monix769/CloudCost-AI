import React from 'react';
import { cloudAccountsList } from '../utils/mockFinOpsData';
import { Cloud, CheckCircle, RefreshCw, Cpu, Layers, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function CloudProviderSummary() {
  const getProviderTheme = (provider) => {
    switch (provider) {
      case 'AWS':
        return {
          border: 'border-amber-500/30 hover:border-amber-500/50',
          badge: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
          bar: 'bg-amber-500',
          accent: 'text-amber-400',
        };
      case 'GCP':
        return {
          border: 'border-sky-500/30 hover:border-sky-500/50',
          badge: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
          bar: 'bg-sky-500',
          accent: 'text-sky-400',
        };
      case 'AZURE':
        return {
          border: 'border-indigo-500/30 hover:border-indigo-500/50',
          badge: 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30',
          bar: 'bg-indigo-500',
          accent: 'text-indigo-400',
        };
      default:
        return {
          border: 'border-slate-800',
          badge: 'bg-slate-800 text-slate-300',
          bar: 'bg-blue-500',
          accent: 'text-blue-400',
        };
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-bold text-white text-base">Connected Cloud Environments</h3>
          <p className="text-xs text-slate-400">
            Real-time synchronization health and spend allocation per cloud tenant
          </p>
        </div>

        <Link
          to="/cloud-accounts"
          className="text-xs font-semibold text-blue-400 hover:text-blue-300 inline-flex items-center space-x-1"
        >
          <span>View All Accounts</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {cloudAccountsList.map((acc) => {
          const theme = getProviderTheme(acc.provider);
          const utilPercent = Math.round((acc.currentSpend / acc.monthlyBudget) * 100);

          return (
            <div
              key={acc.id}
              className={`glass-panel glass-panel-hover rounded-2xl p-5 border ${theme.border} space-y-4 flex flex-col justify-between`}
            >
              <div>
                {/* Header */}
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${theme.badge}`}>
                      {acc.provider}
                    </span>
                    <h4 className="font-bold text-white text-sm pt-1">{acc.name}</h4>
                    <p className="text-[11px] font-mono text-slate-400">{acc.accountId}</p>
                  </div>

                  <span className="flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-mono font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                    <span>{acc.syncStatus}</span>
                  </span>
                </div>

                {/* Spend & Budget */}
                <div className="mt-4 pt-3 border-t border-slate-800/80 space-y-2">
                  <div className="flex items-baseline justify-between">
                    <span className="text-xs text-slate-400">Month-to-Date Spend</span>
                    <span className="text-lg font-bold font-mono text-white">
                      ${acc.currentSpend.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </span>
                  </div>

                  {/* Progress */}
                  <div className="space-y-1">
                    <div className="flex items-center justify-between text-[11px] font-mono">
                      <span className="text-slate-400">Budget: ${acc.monthlyBudget.toLocaleString()}</span>
                      <span className={`font-semibold ${theme.accent}`}>{utilPercent}%</span>
                    </div>
                    <div className="w-full h-1.5 rounded-full bg-navy-900 border border-slate-800 overflow-hidden">
                      <div
                        className={`h-full rounded-full ${theme.bar}`}
                        style={{ width: `${Math.min(utilPercent, 100)}%` }}
                      ></div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Footer Meta */}
              <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400 font-mono">
                <span className="flex items-center space-x-1.5">
                  <Cpu className="w-3.5 h-3.5 text-slate-400" />
                  <span>{acc.activeResources} Resources</span>
                </span>
                <span>Synced {acc.lastSync}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
