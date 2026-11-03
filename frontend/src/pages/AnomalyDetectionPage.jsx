import React, { useState, useEffect } from 'react';
import anomalyService from '../services/anomalyService';
import { ShieldAlert, AlertTriangle, CheckCircle, RefreshCw, Filter, Check, Loader2, AlertCircle } from 'lucide-react';

export default function AnomalyDetectionPage() {
  const [anomalies, setAnomalies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updatingId, setUpdatingId] = useState(null);
  const [selectedSeverity, setSelectedSeverity] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  const fetchAnomalies = async () => {
    try {
      setLoading(true);
      setError(null);
      const params = {
        severity: selectedSeverity !== 'ALL' ? selectedSeverity : undefined,
        status: selectedStatus !== 'ALL' ? selectedStatus : undefined,
      };
      const data = await anomalyService.getAnomalies(params);
      setAnomalies(data || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch anomaly records');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnomalies();
  }, [selectedSeverity, selectedStatus]);

  const handleUpdateStatus = async (id, newStatus) => {
    try {
      setUpdatingId(id);
      await anomalyService.updateAnomalyStatus(id, newStatus);
      await fetchAnomalies();
    } catch (err) {
      alert('Error updating status: ' + err.message);
    } finally {
      setUpdatingId(null);
    }
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
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>Cost Anomaly Management</span>
            <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30">
              {anomalies.length} Persisted Records
            </span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Persisted MySQL anomaly records with workflow state transitions (Acknowledge, Resolve)
          </p>
        </div>

        <div className="flex items-center space-x-2">
          {/* Severity filter */}
          <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono">
            {['ALL', 'CRITICAL', 'HIGH', 'MEDIUM'].map((sev) => (
              <button
                key={sev}
                onClick={() => setSelectedSeverity(sev)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-all ${
                  selectedSeverity === sev
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {sev}
              </button>
            ))}
          </div>

          <button
            onClick={fetchAnomalies}
            title="Refresh anomalies"
            className="p-2 rounded-xl bg-navy-900 border border-slate-800 text-slate-400 hover:text-white"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-blue-400' : ''}`} />
          </button>
        </div>
      </div>

      {/* Error alert */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Loading Skeletons */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2].map((n) => (
            <div key={n} className="glass-panel rounded-2xl p-6 border border-white/[0.08] animate-pulse space-y-3">
              <div className="h-6 w-1/3 bg-slate-800 rounded"></div>
              <div className="h-16 bg-slate-800/60 rounded"></div>
            </div>
          ))}
        </div>
      ) : anomalies.length === 0 ? (
        /* Empty State */
        <div className="glass-panel rounded-2xl p-12 border border-white/[0.08] text-center space-y-3">
          <CheckCircle className="w-12 h-12 text-emerald-500 mx-auto" />
          <h3 className="text-base font-semibold text-white">No Spending Anomalies Detected</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            All cloud expenditures are operating within historical baselines and expected variance thresholds.
          </p>
        </div>
      ) : (
        /* Anomalies List */
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
                  <h3 className="font-bold text-white text-base">{item.serviceName}</h3>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                    {item.provider || 'CLOUD'}
                  </span>
                  {item.accountName && (
                    <span className="text-[11px] text-slate-400 font-mono">
                      ({item.accountName})
                    </span>
                  )}
                </div>

                <div className="flex items-center space-x-3 text-xs font-mono">
                  <span className="text-slate-400">
                    {item.detectedAt ? new Date(item.detectedAt).toLocaleString() : 'Recent'}
                  </span>
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
                  <span className="text-slate-400 block text-[10px]">Expected Cost</span>
                  <span className="text-base font-bold text-slate-200">
                    ${Number(item.expectedCost || 0).toFixed(2)}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Actual Detected Spend</span>
                  <span className="text-base font-bold text-rose-400">
                    ${Number(item.actualCost || 0).toFixed(2)}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Deviation Variance</span>
                  <span className="text-base font-bold text-amber-400">
                    +{Number(item.deviationPercentage || 0).toFixed(1)}%
                  </span>
                </div>
              </div>

              {/* Root Cause Summary */}
              {item.rootCauseSummary && (
                <div className="text-xs space-y-1">
                  <span className="text-[10px] font-mono uppercase font-bold text-slate-400 tracking-wider">
                    FinOps Root Cause Diagnostic:
                  </span>
                  <p className="text-slate-300 leading-relaxed bg-navy-950/60 p-3 rounded-xl border border-white/[0.04]">
                    {item.rootCauseSummary}
                  </p>
                </div>
              )}

              {/* Resolution Actions */}
              <div className="pt-2 flex items-center justify-end space-x-2 text-xs">
                {item.status !== 'RESOLVED' ? (
                  <>
                    {item.status !== 'ACKNOWLEDGED' && (
                      <button
                        onClick={() => handleUpdateStatus(item.id, 'ACKNOWLEDGED')}
                        disabled={updatingId === item.id}
                        className="px-3 py-1.5 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 font-semibold flex items-center space-x-1"
                      >
                        {updatingId === item.id && <Loader2 className="w-3 h-3 animate-spin" />}
                        <span>Acknowledge</span>
                      </button>
                    )}
                    <button
                      onClick={() => handleUpdateStatus(item.id, 'RESOLVED')}
                      disabled={updatingId === item.id}
                      className="px-4 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold shadow-md flex items-center space-x-1"
                    >
                      {updatingId === item.id ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <Check className="w-3.5 h-3.5" />
                      )}
                      <span>Mark Resolved</span>
                    </button>
                  </>
                ) : (
                  <span className="text-emerald-400 text-xs font-semibold flex items-center space-x-1 font-mono">
                    <CheckCircle className="w-3.5 h-3.5" />
                    <span>Closed &amp; Resolved</span>
                  </span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
