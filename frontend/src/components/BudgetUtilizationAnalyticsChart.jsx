import React, { useState, useEffect } from 'react';
import costAnalyticsService from '../services/costAnalyticsService';
import { formatCurrency } from '../utils/formatters';
import { ShieldAlert, CheckCircle2, AlertTriangle, XCircle, Flame, Target } from 'lucide-react';

export default function BudgetUtilizationAnalyticsChart({ currency = 'USD' }) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadBudgetAnalytics() {
      try {
        setLoading(true);
        const res = await costAnalyticsService.getBudgetUtilization({ currency });
        setData(res);
      } catch (err) {
        console.error('Failed to load budget utilization analytics:', err);
      } finally {
        setLoading(false);
      }
    }
    loadBudgetAnalytics();
  }, [currency]);

  if (loading) {
    return (
      <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-emerald-500"></div>
      </div>
    );
  }

  const overallHealth = data?.overallHealthStatus || 'HEALTHY';
  const utilization = data?.overallUtilizationPercentage || 0;
  const budgets = data?.budgetBreakdowns || [];

  const getHealthBadge = (status) => {
    switch (status) {
      case 'EXCEEDED':
        return {
          icon: XCircle,
          label: 'Exceeded',
          color: 'bg-rose-500/10 text-rose-300 border-rose-500/30',
          barColor: 'bg-rose-500',
        };
      case 'CRITICAL':
        return {
          icon: AlertTriangle,
          label: 'Critical',
          color: 'bg-amber-500/10 text-amber-300 border-amber-500/30',
          barColor: 'bg-amber-500',
        };
      case 'WARNING':
        return {
          icon: AlertTriangle,
          label: 'Warning',
          color: 'bg-yellow-500/10 text-yellow-300 border-yellow-500/30',
          barColor: 'bg-yellow-500',
        };
      case 'HEALTHY':
      default:
        return {
          icon: CheckCircle2,
          label: 'Healthy',
          color: 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30',
          barColor: 'bg-emerald-500',
        };
    }
  };

  const overallBadge = getHealthBadge(overallHealth);
  const OverallIcon = overallBadge.icon;

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <Target className="w-5 h-5 text-emerald-400" />
            <h3 className="font-bold text-white text-base">Budget Utilization & Run-Rate</h3>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Active consumption tracking against organizational spend thresholds
          </p>
        </div>

        <span className={`inline-flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-mono font-semibold border ${overallBadge.color}`}>
          <OverallIcon className="w-3.5 h-3.5" />
          <span>{overallBadge.label} ({utilization}%)</span>
        </span>
      </div>

      {/* Aggregate KPI Strip */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800">
          <span className="text-[10px] font-mono uppercase text-slate-400 font-semibold">Allocated Budget</span>
          <div className="text-base font-bold font-mono text-white mt-0.5">
            {formatCurrency(data?.totalAllocatedBudget, currency)}
          </div>
        </div>

        <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800">
          <span className="text-[10px] font-mono uppercase text-slate-400 font-semibold">Current Spend</span>
          <div className="text-base font-bold font-mono text-white mt-0.5">
            {formatCurrency(data?.totalCurrentSpend, currency)}
          </div>
        </div>

        <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800">
          <span className="text-[10px] font-mono uppercase text-slate-400 font-semibold">Remaining</span>
          <div className="text-base font-bold font-mono text-emerald-400 mt-0.5">
            {formatCurrency(data?.totalRemainingBudget, currency)}
          </div>
        </div>

        <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800">
          <span className="text-[10px] font-mono uppercase text-slate-400 font-semibold">Projected EOP Spend</span>
          <div className="text-base font-bold font-mono text-purple-300 mt-0.5">
            {formatCurrency(data?.estimatedEndOfPeriodSpend, currency)}
          </div>
        </div>
      </div>

      {/* Overall Progress Bar */}
      <div className="space-y-1.5">
        <div className="flex justify-between text-xs text-slate-400">
          <span>Overall Consumption</span>
          <span className="font-mono text-slate-200 font-semibold">{utilization}%</span>
        </div>
        <div className="h-2.5 w-full bg-slate-800 rounded-full overflow-hidden">
          <div
            className={`h-full transition-all duration-500 rounded-full ${overallBadge.barColor}`}
            style={{ width: `${Math.min(100, utilization)}%` }}
          />
        </div>
      </div>

      {/* Individual Budget Cards */}
      <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
        {budgets.length === 0 ? (
          <div className="text-xs text-slate-500 font-mono py-4 text-center">
            No active budget limits configured for this organization.
          </div>
        ) : (
          budgets.map((b) => {
            const bBadge = getHealthBadge(b.healthStatus);
            return (
              <div key={b.budgetId} className="p-3 rounded-xl bg-navy-900/40 border border-slate-800/80 space-y-2">
                <div className="flex items-center justify-between text-xs">
                  <div className="font-semibold text-slate-200 truncate pr-2">{b.budgetName}</div>
                  <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold border ${bBadge.color}`}>
                    {b.utilizationPercentage}%
                  </span>
                </div>

                <div className="h-1.5 w-full bg-slate-800 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full ${bBadge.barColor}`}
                    style={{ width: `${Math.min(100, b.utilizationPercentage)}%` }}
                  />
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-400 font-mono">
                  <span>Spend: {formatCurrency(b.currentSpend, currency)} / {formatCurrency(b.amountLimit, currency)}</span>
                  <span>EOP: {formatCurrency(b.estimatedEndOfPeriodSpend, currency)}</span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

