import React from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

export default function KpiCard({
  title,
  value,
  subtitle,
  changePercent,
  changeType = 'increase',
  icon: Icon,
  progress,
  progressColor = 'bg-blue-500',
  accentColor = 'blue',
  badgeText,
}) {
  const accentStyles = {
    blue: 'border-blue-500/20 bg-blue-500/10 text-blue-400',
    amber: 'border-amber-500/20 bg-amber-500/10 text-amber-400',
    emerald: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400',
    purple: 'border-purple-500/20 bg-purple-500/10 text-purple-400',
    rose: 'border-rose-500/20 bg-rose-500/10 text-rose-400',
  };

  const badgeStyle = accentStyles[accentColor] || accentStyles.blue;

  return (
    <div className="glass-panel glass-panel-hover rounded-2xl p-5 border border-white/[0.08] relative overflow-hidden flex flex-col justify-between">
      {/* Top row: Label & Icon */}
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <div className="flex items-center space-x-2">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400 font-mono">
              {title}
            </span>
            {badgeText && (
              <span className="text-[9px] px-1.5 py-0.5 rounded font-mono font-bold bg-slate-800 text-slate-300 border border-slate-700">
                {badgeText}
              </span>
            )}
          </div>
          <h3 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            {value}
          </h3>
        </div>

        {Icon && (
          <div className={`p-3 rounded-xl border ${badgeStyle} shadow-sm`}>
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>

      {/* Progress meter (optional for budgets) */}
      {progress !== undefined && (
        <div className="mt-4 space-y-1.5">
          <div className="flex items-center justify-between text-[11px]">
            <span className="text-slate-400">Utilization</span>
            <span className="font-mono font-bold text-slate-200">{progress}%</span>
          </div>
          <div className="w-full h-2 rounded-full bg-navy-900 border border-slate-800 overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${progressColor}`}
              style={{ width: `${Math.min(progress, 100)}%` }}
            ></div>
          </div>
        </div>
      )}

      {/* Bottom row: Subtitle & Trend */}
      <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs">
        <p className="text-slate-400 text-[11px] truncate">{subtitle}</p>

        {changePercent !== undefined && (
          <div
            className={`flex items-center space-x-1 font-mono text-[11px] font-semibold px-2 py-0.5 rounded-full ${
              changeType === 'increase'
                ? 'bg-amber-500/10 text-amber-300 border border-amber-500/20'
                : 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
            }`}
          >
            {changeType === 'increase' ? (
              <TrendingUp className="w-3 h-3" />
            ) : changeType === 'decrease' ? (
              <TrendingDown className="w-3 h-3" />
            ) : (
              <Minus className="w-3 h-3" />
            )}
            <span>{changePercent > 0 ? `+${changePercent}%` : `${changePercent}%`}</span>
          </div>
        )}
      </div>
    </div>
  );
}
