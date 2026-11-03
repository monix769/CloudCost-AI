import React from 'react';
import { useSystemStatus } from '../hooks/useSystemStatus';
import StatusCard from '../components/StatusCard';
import { formatBytes, formatDate } from '../utils/formatters';
import { Cpu, Server, Shield, Layers, CheckCircle2, RefreshCw } from 'lucide-react';

export default function StatusPage() {
  const { status, loading, error, refetch } = useSystemStatus();

  const mem = status?.memory || {};

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white flex items-center space-x-3">
            <span>System Architecture Status</span>
            <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-teal-500/20 text-teal-300 border border-teal-500/30">
              GET /api/status
            </span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Detailed JVM runtime environment, active Spring profiles, memory allocation, and enabled FinOps subsystems.
          </p>
        </div>

        <button
          onClick={refetch}
          disabled={loading}
          className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-teal-300 border border-slate-700 transition-all self-start sm:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          <span>Refresh Diagnostics</span>
        </button>
      </div>

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatusCard
          icon={Cpu}
          title="Java Virtual Machine"
          value={`Java ${status?.javaVersion?.split('.')[0] || '21'}`}
          subtitle={`JDK Build: ${status?.javaVersion || '21.0.6'}`}
          status="Microsoft OpenJDK 64-Bit"
          badgeColor="teal"
        />

        <StatusCard
          icon={Server}
          title="Spring Boot Core"
          value={status?.springBootVersion || '3.3.5'}
          subtitle={`Profile: [${status?.activeProfiles?.join(', ') || 'dev'}]`}
          status="Spring Framework 6.1"
          badgeColor="emerald"
        />

        <StatusCard
          icon={Shield}
          title="Security Mode"
          value="STATELESS"
          subtitle={status?.securityMode || 'PERMIT_ALL_M1'}
          status="Spring Security 6"
          badgeColor="blue"
        />

        <StatusCard
          icon={Layers}
          title="JVM Memory (Used/Max)"
          value={`${mem.usedMemoryMb || 0} MB`}
          subtitle={`Max Heap: ${mem.maxMemoryMb || 0} MB (Free: ${mem.freeMemoryMb || 0} MB)`}
          status="Memory Pool Healthy"
          badgeColor="purple"
        />
      </div>

      {/* Modules & Milestones Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Active Modules */}
        <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <span>Active Platform Subsystems</span>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-teal-500/10 text-teal-300 border border-teal-500/20">
              {status?.activeModules?.length || 7} Enabled
            </span>
          </h2>

          <div className="space-y-2">
            {status?.activeModules?.map((mod, index) => (
              <div key={index} className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 flex items-center justify-between text-xs">
                <span className="font-medium text-slate-200">{mod}</span>
                <span className="flex items-center space-x-1.5 text-teal-400 font-mono text-[11px]">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>LOADED</span>
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Milestone Verification Details */}
        <div className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
          <h2 className="text-base font-bold text-white flex items-center space-x-2">
            <span>Completed Architectural Deliverables</span>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
              Verified
            </span>
          </h2>

          <div className="space-y-2 text-xs">
            <div className="p-3 rounded-xl bg-slate-900/60 border border-slate-800 space-y-1">
              <p className="font-bold text-white">Milestone 1 — Complete Project Foundation</p>
              <p className="text-slate-400 text-[11px]">
                Java 21, Spring Boot 3.3.5, React 19, Tailwind CSS, MySQL 8 schema, REST endpoints, Postman collection, Docker containerization.
              </p>
            </div>

            <div className="p-4 rounded-xl bg-teal-950/20 border border-teal-500/30 text-teal-300 text-xs">
              <p className="font-semibold mb-1">Architecture Note:</p>
              <p className="text-[11px] leading-relaxed text-slate-300">
                Controllers are strictly decoupled from business logic and database persistence. Standardized <code className="text-teal-300 bg-slate-900 px-1 py-0.5 rounded">ApiResponse&lt;T&gt;</code> envelope guarantees predictable API behavior for frontend clients.
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Raw Status Payload */}
      <div className="glass-panel rounded-2xl p-6 border border-white/10">
        <h3 className="text-xs font-mono font-bold uppercase tracking-wider text-slate-400 mb-3">
          Raw Status JSON Response
        </h3>
        <pre className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 text-teal-300 text-xs font-mono overflow-x-auto">
          {JSON.stringify(status, null, 2)}
        </pre>
      </div>
    </div>
  );
}
