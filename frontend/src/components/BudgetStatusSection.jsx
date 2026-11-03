import React, { useState, useEffect } from 'react';
import { departmentBudgets as fallbackMock } from '../utils/mockFinOpsData';
import budgetService from '../services/budgetService';
import { Target, AlertTriangle, CheckCircle, ChevronRight, RefreshCw } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function BudgetStatusSection({ refreshTrigger }) {
  const [budgets, setBudgets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [useLive, setUseLive] = useState(false);

  useEffect(() => {
    async function loadBudgets() {
      try {
        setLoading(true);
        const data = await budgetService.getBudgets();
        if (data && data.length > 0) {
          setBudgets(data);
          setUseLive(true);
        } else {
          setUseLive(false);
        }
      } catch (err) {
        console.warn('Failed to load live budgets, using baseline:', err);
        setUseLive(false);
      } finally {
        setLoading(false);
      }
    }
    loadBudgets();
  }, [refreshTrigger]);

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4 flex flex-col justify-between h-full">
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="font-bold text-white text-base">Department Budget Health</h3>
            <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold border ${
              useLive
                ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20'
                : 'bg-blue-500/10 text-blue-300 border-blue-500/20'
            }`}>
              {useLive ? '● Live Calculation' : 'Demonstration'}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Spending vs. limits dynamically calculated against billing records
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
        {loading ? (
          <div className="py-8 flex items-center justify-center text-xs text-slate-500">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-400 mr-2" />
            Loading budget utilization...
          </div>
        ) : useLive ? (
          budgets.map((b) => {
            const spent = Number(b.currentSpend || 0);
            const limit = Number(b.amountLimit || 0);
            const util = Number(b.utilizationPercentage || 0);
            const isWarning = b.isNearThreshold || util >= 80;
            const isExceeded = b.isExceeded || util >= 100;

            return (
              <div key={b.id} className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 space-y-2">
                <div className="flex items-center justify-between">
                  <div>
                    <h4 className="font-semibold text-white text-xs">{b.name}</h4>
                    <p className="text-[10px] text-slate-400 font-mono">Period: {b.period}</p>
                  </div>
                  <div className="text-right">
                    <span
                      className={`font-mono font-bold text-xs ${
                        isExceeded ? 'text-rose-400' : isWarning ? 'text-amber-400' : 'text-emerald-400'
                      }`}
                    >
                      ${spent.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} / ${limit.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </span>
                    <span className="block text-[10px] font-mono text-slate-400">
                      {util.toFixed(1)}% utilized
                    </span>
                  </div>
                </div>

                {/* Progress Bar */}
                <div className="w-full h-1.5 rounded-full bg-slate-950 overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${
                      isExceeded
                        ? 'bg-rose-500'
                        : isWarning
                        ? 'bg-amber-500'
                        : 'bg-emerald-500'
                    }`}
                    style={{ width: `${Math.min(util, 100)}%` }}
                  ></div>
                </div>
              </div>
            );
          })
        ) : (
          departmentBudgets.map((dept) => {
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
          })
        )}
      </div>

      <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400 font-mono">
        <span>Threshold: <strong className="text-amber-300">80%</strong></span>
        <span>Limit Cap: <strong className="text-rose-400">100%</strong></span>
      </div>
    </div>
  );
}
