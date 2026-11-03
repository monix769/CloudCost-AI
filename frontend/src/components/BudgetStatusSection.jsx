import React from 'react';
import { departmentBudgets } from '../utils/mockFinOpsData';
import { Target, AlertTriangle, CheckCircle, ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function BudgetStatusSection() {
  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4 flex flex-col justify-between h-full">
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <h3 className="font-bold text-white text-base">Department Budget Health</h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Spending vs. limits allocated by team cost center
          </p>
        </div>

        <Link
          to="/budgets"
          className="text-xs font-semibold text-blue-400 hover:text-blue-300 inline-flex items-center space-x-1"
        >
          <span>Manage Budgets</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      <div className="space-y-4">
        {departmentBudgets.map((dept) => {
          const isWarning = dept.percentage >= 80;
          return (
            <div key={dept.department} className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between">
                <div>
                  <h4 className="font-semibold text-white text-xs">{dept.department}</h4>
                  <p className="text-[10px] text-slate-400">Lead: {dept.lead}</p>
                </div>
                <div className="text-right">
                  <span
                    className={`font-mono font-bold text-xs ${
                      isWarning ? 'text-amber-400' : 'text-emerald-400'
                    }`}
                  >
                    ${dept.spent.toLocaleString()} / ${dept.allocated.toLocaleString()}
                  </span>
                  <span className="block text-[10px] font-mono text-slate-400">
                    {dept.percentage.toFixed(1)}% utilized
                  </span>
                </div>
              </div>

              {/* Progress Bar */}
              <div className="w-full h-1.5 rounded-full bg-slate-950 overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    dept.percentage >= 90
                      ? 'bg-rose-500'
                      : dept.percentage >= 80
                      ? 'bg-amber-500'
                      : 'bg-emerald-500'
                  }`}
                  style={{ width: `${Math.min(dept.percentage, 100)}%` }}
                ></div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
        <span>Alert Threshold: <strong className="text-amber-300 font-mono">80%</strong></span>
        <span>Hard Cap: <strong className="text-rose-400 font-mono">100%</strong></span>
      </div>
    </div>
  );
}
