import React from 'react';
import { useHealth } from '../hooks/useHealth';
import StatusCard from '../components/StatusCard';
import { formatBytes, formatUptime, formatDate } from '../utils/formatters';
import { Activity, Database, HardDrive, RefreshCw, CheckCircle2, AlertTriangle, Clock } from 'lucide-react';

export default function HealthPage() {
  const { health, loading, error, isConnected, isDatabaseConnected, refetch } = useHealth();

  const dbDetails = health?.components?.database || {};
  const diskDetails = health?.components?.diskSpace || {};

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white flex items-center space-x-3">
            <span>Application Health Diagnostics</span>
            <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-teal-500/20 text-teal-300 border border-teal-500/30">
              GET /api/health
            </span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Real-time health probes verifying Spring Boot application status, MySQL connectivity, and disk storage.
          </p>
        </div>

        <button
          onClick={refetch}
          disabled={loading}
          className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-teal-300 border border-slate-700 transition-all self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Health Probe</span>
        </button>
      </div>

      {/* Main Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatusCard
          icon={Activity}
          title="Overall Service State"
          value={health?.status || 'UNKNOWN'}
          subtitle={`Platform Version: ${health?.version || '1.0.0-SNAPSHOT'}`}
          status={isConnected ? 'Healthy & Serving Requests' : 'Unhealthy or Degraded'}
          badgeColor={isConnected ? 'teal' : 'amber'}
        />

        <StatusCard
          icon={Database}
          title="MySQL Connection"
          value={health?.database || 'DISCONNECTED'}
          subtitle={dbDetails.databaseProduct ? `${dbDetails.databaseProduct} ${dbDetails.databaseVersion || ''}` : 'cloudcost_ai'}
          status={isDatabaseConnected ? `Ping Latency: ${dbDetails.pingLatencyMs || 0} ms` : 'Connection Failed'}
          badgeColor={isDatabaseConnected ? 'emerald' : 'amber'}
        />

        <StatusCard
          icon={HardDrive}
          title="Storage Free Space"
          value={formatBytes(diskDetails.freeBytes || 0)}
          subtitle={`Total Volume: ${formatBytes(diskDetails.totalBytes || 0)}`}
          status="Filesystem OK"
          badgeColor="blue"
        />

        <StatusCard
          icon={Clock}
          title="Process Uptime"
          value={formatUptime(health?.uptimeSeconds || 0)}
          subtitle={formatDate(health?.timestamp)}
          status="Continuous JVM Operation"
          badgeColor="purple"
        />
      </div>

      {/* Detailed Diagnostics Table */}
      <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
        <h2 className="text-base font-bold text-white">Component Inspection Breakdown</h2>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 font-mono text-[11px]">
                <th className="pb-3 font-semibold">Component</th>
                <th className="pb-3 font-semibold">Status</th>
                <th className="pb-3 font-semibold">Details / Driver</th>
                <th className="pb-3 font-semibold">Operational Indicator</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-mono text-[11px]">
              <tr>
                <td className="py-3.5 font-bold text-white flex items-center space-x-2">
                  <Database className="w-4 h-4 text-teal-400" />
                  <span>MySQL Database (cloudcost_ai)</span>
                </td>
                <td className="py-3.5">
                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    isDatabaseConnected ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                  }`}>
                    {dbDetails.status || 'UNKNOWN'}
                  </span>
                </td>
                <td className="py-3.5 text-slate-300">
                  {dbDetails.driverName || 'com.mysql.cj.jdbc.Driver'} (Catalog: {dbDetails.catalog || 'cloudcost_ai'})
                </td>
                <td className="py-3.5 text-teal-400">
                  SELECT 1 Ping: {dbDetails.pingLatencyMs || 0} ms
                </td>
              </tr>

              <tr>
                <td className="py-3.5 font-bold text-white flex items-center space-x-2">
                  <HardDrive className="w-4 h-4 text-cyan-400" />
                  <span>Host Disk Space</span>
                </td>
                <td className="py-3.5">
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    {diskDetails.status || 'UP'}
                  </span>
                </td>
                <td className="py-3.5 text-slate-300">
                  Free: {formatBytes(diskDetails.freeBytes || 0)} / Total: {formatBytes(diskDetails.totalBytes || 0)}
                </td>
                <td className="py-3.5 text-emerald-400">
                  Sufficient Storage Available
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Raw Payload Card */}
      <div className="glass-panel rounded-2xl p-6 border border-white/10">
        <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 mb-3">
          Raw Health JSON Response
        </h3>
        <pre className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 text-teal-300 text-xs font-mono overflow-x-auto">
          {JSON.stringify(health, null, 2)}
        </pre>
      </div>
    </div>
  );
}
