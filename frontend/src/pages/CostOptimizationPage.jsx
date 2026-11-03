import React, { useState } from 'react';
import { optimizationOpportunities } from '../utils/mockFinOpsData';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import { Lightbulb, PiggyBank, ArrowRight, Check, Zap, Layers, Sparkles } from 'lucide-react';

export default function CostOptimizationPage() {
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [appliedIds, setAppliedIds] = useState([]);

  const filtered = optimizationOpportunities.filter(
    (item) => selectedCategory === 'ALL' || item.category === selectedCategory
  );

  const totalSavings = optimizationOpportunities.reduce(
    (acc, curr) => acc + curr.estimatedSavingsMonthly,
    0
  );

  const handleApply = (id) => {
    setAppliedIds([...appliedIds, id]);
  };

  const getEffortBadge = (effort) => {
    switch (effort) {
      case 'VERY_LOW':
        return 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30';
      case 'LOW':
        return 'bg-blue-500/15 text-blue-300 border-blue-500/30';
      case 'MEDIUM':
        return 'bg-amber-500/15 text-amber-300 border-amber-500/30';
      case 'HIGH':
        return 'bg-purple-500/15 text-purple-300 border-purple-500/30';
      default:
        return 'bg-slate-800 text-slate-300';
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <DemoDataDisclaimer />

      {/* Hero Savings Strip */}
      <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/[0.08] shadow-card-elevated relative overflow-hidden flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-purple-500/15 border border-purple-500/30 text-purple-300 text-xs font-semibold">
            <Sparkles className="w-3.5 h-3.5" />
            <span>AI FinOps Cost Recommendation Engine</span>
          </div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white">
            Identified Monthly Savings Opportunity
          </h2>
          <p className="text-xs text-slate-300 max-w-xl leading-relaxed">
            Eliminate idle compute resources, downgrade underutilized instance types, and lock in baseline commitment savings.
          </p>
        </div>

        <div className="p-5 rounded-2xl bg-navy-900/80 border border-purple-500/30 text-center shrink-0">
          <p className="text-[11px] uppercase font-mono font-bold text-slate-400">Total Potential Savings</p>
          <p className="text-3xl sm:text-4xl font-extrabold font-mono text-purple-400 my-1">
            ${totalSavings.toLocaleString()}<span className="text-sm text-slate-400 font-normal"> / mo</span>
          </p>
          <p className="text-xs text-emerald-400 font-semibold font-mono">
            ~${(totalSavings * 12).toLocaleString()} / year reduction
          </p>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex flex-wrap items-center gap-2 pb-2">
        {['ALL', 'RIGHTSIZING', 'IDLE_RESOURCE', 'COMMITMENT', 'STORAGE', 'ARCHITECTURE'].map((cat) => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all font-mono ${
              selectedCategory === cat
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-navy-900 text-slate-400 hover:text-slate-200 border border-slate-800'
            }`}
          >
            {cat.replace('_', ' ')}
          </button>
        ))}
      </div>

      {/* Recommendations Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {filtered.map((item) => {
          const isApplied = appliedIds.includes(item.id);

          return (
            <div
              key={item.id}
              className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4 flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 border border-blue-500/20">
                        {item.provider}
                      </span>
                      <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                        {item.category}
                      </span>
                    </div>
                    <h3 className="font-bold text-white text-base pt-1">{item.title}</h3>
                  </div>

                  <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${getEffortBadge(item.effort)}`}>
                    Effort: {item.effort}
                  </span>
                </div>

                <div className="p-3 my-3 rounded-xl bg-navy-900/60 border border-slate-800 text-xs font-mono text-slate-300">
                  Target Resource: <strong className="text-white">{item.resource}</strong>
                </div>

                <p className="text-xs text-slate-300 leading-relaxed">
                  {item.recommendation}
                </p>
              </div>

              <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between">
                <div>
                  <span className="text-[10px] font-mono text-slate-400 block uppercase">Est. Monthly Savings</span>
                  <span className="text-xl font-bold font-mono text-emerald-400">
                    +${item.estimatedSavingsMonthly.toFixed(2)}
                  </span>
                </div>

                <button
                  onClick={() => handleApply(item.id)}
                  disabled={isApplied}
                  className={`px-4 py-2 rounded-xl text-xs font-semibold transition-all flex items-center space-x-1.5 ${
                    isApplied
                      ? 'bg-emerald-950/60 text-emerald-300 border border-emerald-500/30'
                      : 'bg-blue-600 hover:bg-blue-500 text-white shadow-md'
                  }`}
                >
                  {isApplied ? (
                    <>
                      <Check className="w-3.5 h-3.5" />
                      <span>Action Queued</span>
                    </>
                  ) : (
                    <>
                      <span>Apply Recommendation</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </>
                  )}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
