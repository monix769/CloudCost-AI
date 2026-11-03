import React from 'react';
import { useSystem } from '../context/SystemContext';
import { Server, Database, CheckCircle2, AlertTriangle, RefreshCw } from 'lucide-react';

export default function ConnectionBadge() {
  const { isConnected, isDatabaseConnected, loading, refreshSystemData } = useSystem();

  return (
    <div className="flex items-center space-x-3 text-xs">
      {/* Backend API Status */}
      <div className={`flex items-center space-x-1.5 px-2.5 py-1 rounded-full border ${
        isConnected 
          ? 'bg-emerald-950/60 border-emerald-500/30 text-emerald-300' 
          : 'bg-rose-950/60 border-rose-500/30 text-rose-300'
      }`}>
        <Server className="w-3.5 h-3.5" />
        <span className="font-medium">Backend:</span>
        <span className="font-semibold">{isConnected ? 'ONLINE' : 'OFFLINE'}</span>
        <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-emerald-400 animate-ping' : 'bg-rose-400'}`}></span>
      </div>

      {/* MySQL Database Status */}
      <div className={`flex items-center space-x-1.5 px-2.5 py-1 rounded-full border ${
        isDatabaseConnected 
          ? 'bg-teal-950/60 border-teal-500/30 text-teal-300' 
          : 'bg-amber-950/60 border-amber-500/30 text-amber-300'
      }`}>
        <Database className="w-3.5 h-3.5" />
        <span className="font-medium">MySQL:</span>
        <span className="font-semibold">{isDatabaseConnected ? 'CONNECTED' : 'DISCONNECTED'}</span>
        <span className={`w-2 h-2 rounded-full ${isDatabaseConnected ? 'bg-teal-400' : 'bg-amber-400'}`}></span>
      </div>

      {/* Refresh Button */}
      <button
        onClick={refreshSystemData}
        disabled={loading}
        title="Refresh system connection status"
        className="p-1.5 rounded-lg border border-slate-700 hover:border-teal-500/40 text-slate-400 hover:text-teal-300 transition-colors bg-slate-900/60 disabled:opacity-50"
      >
        <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-teal-400' : ''}`} />
      </button>
    </div>
  );
}
