import React, { useState, useEffect } from 'react';
import costAnalyticsService from '../services/costAnalyticsService';
import { formatCurrency, formatDate } from '../utils/formatters';
import { Lightbulb, AlertOctagon, AlertTriangle, Info, CheckCircle2, ArrowRight, Download, Sparkles } from 'lucide-react';

export default function FinOpsInsightsSection({ currency = 'USD' }) {
  const [insights, setInsights] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchInsights() {
      try {
        setLoading(true);
        const data = await costAnalyticsService.getInsights({ currency });
        setInsights(data);
      } catch (err) {
        console.error('Failed to load FinOps insights:', err);
      } finally {
        setLoading(false);
      }
    }
    fetchInsights();
  }, [currency]);

  const getSeverityStyle = (severity) => {
    switch (severity) {
      case 'CRITICAL':
        return {
          icon: AlertOctagon,
          badge: 'bg-rose-500/10 text-rose-300 border-rose-500/30',
          border: 'border-l-rose-500',
        };
      case 'HIGH':
        return {
          icon: AlertTriangle,
          badge: 'bg-amber-500/10 text-amber-300 border-amber-500/30',
          border: 'border-l-amber-500',
        };
      case 'MEDIUM':
        return {
          icon: Info,
          badge: 'bg-blue-500/10 text-blue-300 border-blue-500/30',
          border: 'border-l-blue-500',
        };
      case 'LOW':
      default:
        return {
          icon: CheckCircle2,
          badge: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30',
          border: 'border-l-emerald-500',
        };
    }
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-800 gap-3">
        <div className="flex items-center space-x-2">
          <Lightbulb className="w-5 h-5 text-amber-400" />
          <div>
            <h3 className="font-bold text-white text-base">Actionable FinOps Insights</h3>
            <p className="text-xs text-slate-400">
              Rule-based statistical detection of cloud spend optimizations and risk vectors
            </p>
          </div>
        </div>

        <button
          onClick={() => costAnalyticsService.downloadExport('insights', { currency })}
          className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 transition-colors self-start sm:self-auto"
        >
          <Download className="w-3.5 h-3.5 text-amber-400" />
          <span>Export Insights CSV</span>
        </button>
      </div>

      {/* Insight List */}
      <div className="space-y-3">
        {loading ? (
          <div className="flex items-center justify-center p-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-500"></div>
          </div>
        ) : insights.length === 0 ? (
          <div className="p-6 text-center text-xs text-slate-500 font-mono">
            No critical anomalies or overspending risks detected for current billing period.
          </div>
        ) : (
          insights.map((insight) => {
            const style = getSeverityStyle(insight.severity);
            const SeverityIcon = style.icon;

            return (
              <div
                key={insight.id}
                className={`p-4 rounded-xl bg-navy-900/60 border border-slate-800 border-l-4 ${style.border} space-y-2.5 transition-all hover:bg-navy-900/80`}
              >
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div className="flex items-center space-x-2">
                    <span className={`inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full text-[10px] font-mono font-semibold border ${style.badge}`}>
                      <SeverityIcon className="w-3 h-3" />
                      <span>{insight.severity}</span>
                    </span>
                    <span className="text-xs font-mono text-slate-500 uppercase">{insight.category}</span>
                  </div>

                  {insight.financialImpact && (
                    <div className="text-xs font-mono font-bold text-white">
                      Financial Impact: <span className="text-amber-400">{formatCurrency(insight.financialImpact, insight.currency || currency)}</span>
                    </div>
                  )}
                </div>

                <div>
                  <h4 className="font-bold text-sm text-slate-100">{insight.title}</h4>
                  <p className="text-xs text-slate-300 mt-1 leading-relaxed">{insight.description}</p>
                </div>

                {/* Recommendation box */}
                <div className="p-2.5 rounded-lg bg-navy-950/80 border border-slate-800/80 flex items-start space-x-2 text-xs">
                  <ArrowRight className="w-3.5 h-3.5 text-blue-400 shrink-0 mt-0.5" />
                  <div className="text-slate-300">
                    <span className="font-semibold text-blue-400">Recommendation: </span>
                    <span>{insight.recommendation}</span>
                  </div>
                </div>

                {/* Footer metadata */}
                <div className="flex flex-wrap items-center gap-3 text-[11px] font-mono text-slate-500 pt-1">
                  {insight.relatedProvider && (
                    <span>Provider: <strong className="text-slate-400">{insight.relatedProvider}</strong></span>
                  )}
                  {insight.relatedService && (
                    <span>Service: <strong className="text-slate-400">{insight.relatedService}</strong></span>
                  )}
                  {insight.relatedAccount && (
                    <span>Account: <strong className="text-slate-400">{insight.relatedAccount}</strong></span>
                  )}
                  <span>Detected: {formatDate(insight.detectionTimestamp)}</span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

