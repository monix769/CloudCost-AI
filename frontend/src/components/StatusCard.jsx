import React from 'react';

export default function StatusCard({ icon: Icon, title, value, subtitle, status, badgeColor = 'teal' }) {
  const colorMap = {
    teal: 'text-teal-400 bg-teal-500/10 border-teal-500/20',
    emerald: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
    blue: 'text-blue-400 bg-blue-500/10 border-blue-500/20',
    purple: 'text-purple-400 bg-purple-500/10 border-purple-500/20',
    amber: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
  };

  const badgeClass = colorMap[badgeColor] || colorMap.teal;

  return (
    <div className="glass-panel glass-panel-hover rounded-xl p-5 border border-white/5 relative overflow-hidden">
      <div className="flex items-start justify-between">
        <div className="space-y-1">
          <p className="text-xs uppercase tracking-wider font-semibold text-slate-400">{title}</p>
          <h3 className="text-2xl font-bold text-white tracking-tight">{value}</h3>
          {subtitle && <p className="text-xs text-slate-400 pt-0.5">{subtitle}</p>}
        </div>
        {Icon && (
          <div className={`p-3 rounded-lg border ${badgeClass}`}>
            <Icon className="w-5 h-5" />
          </div>
        )}
      </div>

      {status && (
        <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs">
          <span className="text-slate-500 font-medium">State</span>
          <span className="font-semibold text-slate-300">{status}</span>
        </div>
      )}
    </div>
  );
}
