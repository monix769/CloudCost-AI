import React from 'react';
import { recentAlerts } from '../utils/mockFinOpsData';
import { AlertTriangle, AlertCircle, Clock, CheckCircle2, ChevronRight, ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function RecentAlertsSection() {
  const getSeverityBadge = (severity) => {
    switch (severity) {
      case 'HIGH':
        return 'bg-rose-500/15 text-rose-300 border-rose-500/30';
      case 'MEDIUM':
        return 'bg-amber-500/15 text-amber-300 border-amber-500/30';
      case 'LOW':
        return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4 flex flex-col justify-between h-full">
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <h3 className="font-bold text-white text-base">Cost Anomalies &amp; Alerts</h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Automated spending spikes and financial threshold notifications
          </p>
        </div>

        <Link
          to="/anomalies"
          className="text-xs font-semibold text-blue-400 hover:text-blue-300 inline-flex items-center space-x-1"
        >
          <span>View All Alerts</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      <div className="space-y-3">
        {recentAlerts.map((alert) => (
          <div
            key={alert.id}
            className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 space-y-1.5 hover:border-slate-700 transition-colors"
          >
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-center space-x-2">
                <span
                  className={`text-[9px] font-mono font-bold px-2 py-0.5 rounded border ${getSeverityBadge(
                    alert.severity
                  )}`}
                >
                  {alert.severity}
                </span>
                <h4 className="font-semibold text-white text-xs">{alert.title}</h4>
              </div>

              <span className="text-[10px] font-mono text-slate-400 shrink-0">{alert.time}</span>
            </div>

            <p className="text-[11px] text-slate-300 leading-relaxed pl-0.5">
              {alert.description}
            </p>

            <div className="pt-1.5 flex items-center justify-between text-[10px] font-mono text-slate-400">
              <span>Service: <strong className="text-slate-200">{alert.service}</strong></span>
              <span className={alert.resolved ? 'text-emerald-400 font-semibold' : 'text-amber-400 font-semibold'}>
                {alert.resolved ? '✓ Resolved' : '● Action Required'}
              </span>
            </div>
          </div>
        ))}
      </div>

      <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
        <span>Anomaly Engine: <strong className="text-emerald-400 font-mono">ACTIVE</strong></span>
        <span>Scanning cadence: <strong className="text-slate-200 font-mono">Hourly</strong></span>
      </div>
    </div>
  );
}
