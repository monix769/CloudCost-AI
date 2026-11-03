import React, { useState } from 'react';
import BudgetForecastChart from '../components/BudgetForecastChart';
import BudgetStatusSection from '../components/BudgetStatusSection';
import budgetService from '../services/budgetService';
import { Target, Plus, Bell, Shield, CheckCircle, AlertTriangle, X, Loader2, AlertCircle } from 'lucide-react';

export default function BudgetManagementPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [newBudget, setNewBudget] = useState({
    name: '',
    period: 'MONTHLY',
    amount: '',
    threshold: 80,
  });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleCreateBudget = async (e) => {
    e.preventDefault();
    if (!newBudget.name || !newBudget.amount) return;

    try {
      setSubmitting(true);
      setError(null);
      await budgetService.createBudget({
        name: newBudget.name,
        amountLimit: parseFloat(newBudget.amount),
        currency: 'USD',
        period: newBudget.period,
        alertThresholdPercentage: parseFloat(newBudget.threshold),
        status: 'ACTIVE',
      });
      setModalOpen(false);
      setNewBudget({ name: '', period: 'MONTHLY', amount: '', threshold: 80 });
      setRefreshTrigger((prev) => prev + 1);
    } catch (err) {
      setError(err.message || 'Failed to create budget in database');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>FinOps Budget Guardrails &amp; Allocations</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Enforce spending limits across business units with dynamic billing utilization metrics
          </p>
        </div>

        <button
          onClick={() => setModalOpen(true)}
          className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Create Department Budget</span>
        </button>
      </div>

      {/* Error alert */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center space-x-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Budget Forecast & Status Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <BudgetForecastChart />
        <BudgetStatusSection refreshTrigger={refreshTrigger} />
      </div>

      {/* Alert Policy Table */}
      <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
        <h3 className="font-bold text-white text-base">Active Budget Threshold Policies</h3>
        <p className="text-xs text-slate-400">
          Configured notification webhooks triggered when actual or forecasted cloud costs cross thresholds
        </p>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-800/80 text-slate-400 font-mono text-[11px]">
                <th className="pb-3 font-semibold">Policy Name</th>
                <th className="pb-3 font-semibold">Scope</th>
                <th className="pb-3 font-semibold">Threshold</th>
                <th className="pb-3 font-semibold">Alert Channel</th>
                <th className="pb-3 font-semibold text-right">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 font-sans">
              <tr>
                <td className="py-3 font-semibold text-white">Soft Warning Trigger</td>
                <td className="py-3 text-slate-300">All Cloud Accounts</td>
                <td className="py-3 font-mono font-bold text-amber-400">80% of Limit</td>
                <td className="py-3 text-slate-300 font-mono">#finops-alerts (Slack)</td>
                <td className="py-3 text-right">
                  <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                    ACTIVE
                  </span>
                </td>
              </tr>
              <tr>
                <td className="py-3 font-semibold text-white">Critical Cap Exceeded</td>
                <td className="py-3 text-slate-300">Engineering &amp; Core Platform</td>
                <td className="py-3 font-mono font-bold text-rose-400">100% of Limit</td>
                <td className="py-3 text-slate-300 font-mono">PagerDuty + Email</td>
                <td className="py-3 text-right">
                  <span className="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                    ACTIVE
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Connection */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.1] shadow-2xl max-w-md w-full space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="font-bold text-white text-base">Create Department Budget</h3>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateBudget} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Budget Name</label>
                <input
                  type="text"
                  placeholder="e.g. Q4 Engineering Cloud Budget"
                  value={newBudget.name}
                  onChange={(e) => setNewBudget({ ...newBudget, name: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Budget Period</label>
                <select
                  value={newBudget.period}
                  onChange={(e) => setNewBudget({ ...newBudget, period: e.target.value })}
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                >
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="ANNUAL">Annual</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Financial Limit Amount ($)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  placeholder="50000.00"
                  value={newBudget.amount}
                  onChange={(e) => setNewBudget({ ...newBudget, amount: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Alert Threshold (%)</label>
                <input
                  type="number"
                  step="1"
                  min="1"
                  max="100"
                  placeholder="80"
                  value={newBudget.threshold}
                  onChange={(e) => setNewBudget({ ...newBudget, threshold: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                />
              </div>

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:bg-slate-800 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-md flex items-center space-x-1"
                >
                  {submitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Save Budget</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
