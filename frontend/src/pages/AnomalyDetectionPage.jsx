import React, { useState } from 'react';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import { ShieldAlert, AlertTriangle, CheckCircle, RefreshCw, Sparkles, Filter, ChevronRight, Check } from 'lucide-react';

export default function AnomalyDetectionPage() {
  const [anomalies, setAnomalies] = useState([
    {
      id: 'anom-1',
      title: 'AWS EC2 Spike in us-east-1',
      service: 'Amazon EC2',
      provider: 'AWS',
      expected: 810.0,
      actual: 1120.0,
      deviation: 38.2,
      severity: 'HIGH',
      detected: '2 hours ago',
      status: 'OPEN',
      rootCause: 'Sudden spin-up of 8 on-demand p3.2xlarge GPU instances during unthrottled ML pipeline test.',
    },
    {
      id: 'anom-2',
      title: 'GCP BigQuery Runaway Query Volume',
      service: 'Google BigQuery',
      provider: 'GCP',
      expected: 120.0,
      actual: 412.0,
      deviation: 243.3,
      severity: 'CRITICAL',
      detected: '1 day ago',
      status: 'ACKNOWLEDGED',
      rootCause: 'Data science pipeline executed non-partitioned full table scans scanning 48 TB of unstructured logs.',
    },
    {
      id: 'anom-3',
      title: 'Azure NAT Gateway Idle Ingress Surcharge',
      service: 'Azure Virtual Network',
      provider: 'AZURE',
      expected: 45.0,
      actual: 140.0,
      deviation: 211.1,
      severity: 'MEDIUM',
      detected: '3 days ago',
      status: 'RESOLVED',
      rootCause: 'Staging VNet gateway retained after load testing suite finished without automated teardown.',
    },
  ]);

  const handleUpdateStatus = (id, newStatus) => {
    setAnomalies(
      anomalies.map((a) => (a.id === id ? { ...a, status: newStatus } : a))
    );
  };

  const getSeverityBadge = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-rose-500/20 text-rose-300 border-rose-500/40';
      case 'HIGH':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/40';
      case 'MEDIUM':
        return 'bg-blue-500/20 text-blue-300 border-blue-500/40';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <DemoDataDisclaimer />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>Machine Learning Anomaly Detection</span>
            <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30">
              Active Probing
            </span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Automated heuristic and statistical algorithms identifying unexpected cloud expenditure spikes
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <span className="text-xs font-mono text-emerald-400 bg-emerald-950/40 border border-emerald-500/30 px-3 py-1.5 rounded-xl flex items-center space-x-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
            <span>Continuous Monitoring</span>
          </span>
        </div>
      </div>

      {/* Anomalies List */}
      <div className="space-y-4">
        {anomalies.map((item) => (
          <div
            key={item.id}
            className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4 transition-all"
          >
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-3">
                <span className={`text-[10px] font-mono font-bold px-2.5 py-1 rounded border ${getSeverityBadge(item.severity)}`}>
                  {item.severity}
                </span>
                <h3 className="font-bold text-white text-base">{item.title}</h3>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                  {item.provider}
                </span>
              </div>

              <div className="flex items-center space-x-3 text-xs font-mono">
                <span className="text-slate-400">{item.detected}</span>
                <span
                  className={`px-2.5 py-0.5 rounded font-bold ${
                    item.status === 'RESOLVED'
                      ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                      : item.status === 'ACKNOWLEDGED'
                      ? 'bg-blue-500/15 text-blue-300 border border-blue-500/30'
                      : 'bg-rose-500/15 text-rose-300 border border-rose-500/30 animate-pulse'
                  }`}
                >
                  {item.status}
                </span>
              </div>
            </div>

            {/* Metrics Strip */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 text-xs font-mono">
              <div>
                <span className="text-slate-400 block text-[10px]">Expected 24h Spend</span>
                <span className="text-base font-bold text-slate-200">${item.expected.toFixed(2)}</span>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">Actual Detected Spend</span>
                <span className="text-base font-bold text-rose-400">${item.actual.toFixed(2)}</span>
              </div>
              <div>
                <span className="text-slate-400 block text-[10px]">Variance Deviation</span>
                <span className="text-base font-bold text-amber-400">+{item.deviation}%</span>
              </div>
            </div>

            {/* Root Cause Summary */}
            <div className="text-xs space-y-1">
              <span className="text-[10px] font-mono uppercase font-bold text-slate-400 tracking-wider">
                Root Cause Diagnostic Note:
              </span>
              <p className="text-slate-300 leading-relaxed bg-navy-950/60 p-3 rounded-xl border border-white/[0.04]">
                {item.rootCause}
              </p>
            </div>

            {/* Resolution Actions */}
            <div className="pt-2 flex items-center justify-end space-x-2 text-xs">
              {item.status !== 'RESOLVED' ? (
                <>
                  <button
                    onClick={() => handleUpdateStatus(item.id, 'ACKNOWLEDGED')}
                    className="px-3 py-1.5 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 font-semibold"
                  >
                    Acknowledge
                  </button>
                  <button
                    onClick={() => handleUpdateStatus(item.id, 'RESOLVED')}
                    className="px-4 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold shadow-md flex items-center space-x-1"
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>Mark Resolved</span>
                  </button>
                </>
              ) : (
                <span className="text-emerald-400 text-xs font-semibold flex items-center space-x-1">
                  <CheckCircle className="w-3.5 h-3.5" />
                  <span>Closed &amp; Archived</span>
                </span>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
