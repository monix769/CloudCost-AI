import React, { useState, useEffect } from 'react';
import cloudAccountService from '../services/cloudAccountService';
import { Cloud, CheckCircle, RefreshCw, Cpu, Layers, ExternalLink, ShieldCheck, Lock, AlertCircle } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function CloudProviderSummary() {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    const fetchAccounts = async () => {
      try {
        const data = await cloudAccountService.getAccounts();
        if (mounted) setAccounts(data || []);
      } catch (err) {
        if (mounted) setAccounts([]);
      } finally {
        if (mounted) setLoading(false);
      }
    };
    fetchAccounts();
    return () => {
      mounted = false;
    };
  }, []);

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
            Real-time multi-cloud ingestion health and credential security per cloud tenant
          </p>
        </div>

        <Link
          to="/cloud-accounts"
          className="text-xs font-semibold text-blue-400 hover:text-blue-300 inline-flex items-center space-x-1"
        >
          <span>Manage Accounts</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </Link>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[1, 2, 3].map((n) => (
            <div key={n} className="glass-panel rounded-2xl p-5 border border-white/[0.08] animate-pulse space-y-3">
              <div className="h-5 w-20 bg-slate-800 rounded"></div>
              <div className="h-6 w-36 bg-slate-800 rounded"></div>
              <div className="h-12 bg-slate-800/60 rounded"></div>
            </div>
          ))}
        </div>
      ) : accounts.length === 0 ? (
        <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] text-center space-y-2">
          <p className="text-sm font-semibold text-white">No Cloud Accounts Configured</p>
          <p className="text-xs text-slate-400">
            Navigate to Cloud Accounts to connect AWS, Azure, or GCP credentials.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {accounts.map((acc) => {
            const theme = getProviderTheme(acc.provider);
            const isConnected = acc.connectionStatus === 'CONNECTED';

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
                      <h4 className="font-bold text-white text-sm pt-1">{acc.accountName}</h4>
                      <p className="text-[11px] font-mono text-slate-400">{acc.accountId}</p>
                    </div>

                    <span
                      className={`flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-mono font-semibold ${
                        isConnected
                          ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                          : 'bg-slate-800 text-slate-400 border border-slate-700'
                      }`}
                    >
                      {isConnected && <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>}
                      <span>{acc.connectionStatus || 'DISCONNECTED'}</span>
                    </span>
                  </div>

                  {/* Encryption & Ingestion Details */}
                  <div className="mt-4 pt-3 border-t border-slate-800/80 space-y-2 text-xs font-mono">
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">Security:</span>
                      {acc.credentialsConfigured ? (
                        <span className="text-emerald-400 flex items-center space-x-1">
                          <Lock className="w-3 h-3" />
                          <span>AES-256-GCM ({acc.keyVersion || 'v1'})</span>
                        </span>
                      ) : (
                        <span className="text-slate-500">Unencrypted Keys</span>
                      )}
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">Sync Status:</span>
                      <span className="text-blue-400 font-semibold">{acc.syncStatus || 'PENDING'}</span>
                    </div>
                  </div>
                </div>

                {/* Footer Meta */}
                <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-[11px] text-slate-400 font-mono">
                  <span className="flex items-center space-x-1 text-slate-400">
                    <ShieldCheck className="w-3.5 h-3.5 text-blue-400" />
                    <span>SPI Ingestion</span>
                  </span>
                  <span>{acc.lastSyncedAt ? `Synced ${new Date(acc.lastSyncedAt).toLocaleDateString()}` : 'Never Synced'}</span>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
