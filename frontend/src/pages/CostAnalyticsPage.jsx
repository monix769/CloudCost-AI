import React, { useState } from 'react';
import SpendingTrendChart from '../components/SpendingTrendChart';
import ServiceBreakdownChart from '../components/ServiceBreakdownChart';
import ProviderDistributionChart from '../components/ProviderDistributionChart';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import { Filter, Calendar, BarChart3, Download, Layers, TrendingUp } from 'lucide-react';

export default function CostAnalyticsPage() {
  const [activeProvider, setActiveProvider] = useState('ALL');
  const [activeCategory, setActiveCategory] = useState('ALL');

  return (
    <div className="space-y-6 animate-fadeIn">
      <DemoDataDisclaimer />

      {/* Filter Toolbar */}
      <div className="glass-panel rounded-2xl p-4 border border-white/[0.08] shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center space-x-2 text-xs text-slate-400 font-mono">
            <Filter className="w-3.5 h-3.5 text-blue-400" />
            <span>Provider:</span>
          </div>
          <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono">
            {['ALL', 'AWS', 'GCP', 'AZURE'].map((prov) => (
              <button
                key={prov}
                onClick={() => setActiveProvider(prov)}
                className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                  activeProvider === prov
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {prov}
              </button>
            ))}
          </div>

          <div className="hidden sm:flex items-center space-x-2 text-xs text-slate-400 font-mono ml-2">
            <span>Category:</span>
          </div>
          <div className="hidden sm:flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono">
            {['ALL', 'Compute', 'Storage', 'Database', 'Network'].map((cat) => (
              <button
                key={cat}
                onClick={() => setActiveCategory(cat)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-all ${
                  activeCategory === cat
                    ? 'bg-blue-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        <button className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 transition-colors self-start sm:self-auto">
          <Download className="w-3.5 h-3.5 text-blue-400" />
          <span>Export Analytics</span>
        </button>
      </div>

      {/* Analytics KPI strip */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08]">
          <p className="text-xs uppercase font-mono text-slate-400 font-semibold">Average Daily Spend</p>
          <h3 className="text-2xl font-bold font-mono text-white mt-1">$1,428.34</h3>
          <p className="text-[11px] text-emerald-400 font-semibold mt-1">● Optimal velocity</p>
        </div>

        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08]">
          <p className="text-xs uppercase font-mono text-slate-400 font-semibold">Projected Month-End</p>
          <h3 className="text-2xl font-bold font-mono text-white mt-1">$48,200.00</h3>
          <p className="text-[11px] text-slate-400 mt-1">Within $50k monthly budget</p>
        </div>

        <div className="glass-panel rounded-2xl p-5 border border-white/[0.08]">
          <p className="text-xs uppercase font-mono text-slate-400 font-semibold">Top Cost Category</p>
          <h3 className="text-2xl font-bold text-white mt-1">Compute (62%)</h3>
          <p className="text-[11px] text-blue-400 font-semibold mt-1">EC2, GKE, and Azure VMs</p>
        </div>
      </div>

      {/* Primary Analytics Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <SpendingTrendChart />
        <ServiceBreakdownChart />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <ProviderDistributionChart />

        <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
          <h3 className="font-bold text-white text-base">FinOps Unit Economics</h3>
          <p className="text-xs text-slate-400">
            Key cost performance indicators correlated with business metrics
          </p>

          <div className="space-y-3 font-mono text-xs">
            <div className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 flex justify-between items-center">
              <span className="text-slate-300">Cost Per Active Daily User:</span>
              <span className="font-bold text-emerald-400">$0.042 (-8% MoM)</span>
            </div>
            <div className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 flex justify-between items-center">
              <span className="text-slate-300">Cost Per API Transaction (100k):</span>
              <span className="font-bold text-blue-400">$1.18</span>
            </div>
            <div className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 flex justify-between items-center">
              <span className="text-slate-300">Storage Unit Cost (per GB):</span>
              <span className="font-bold text-slate-200">$0.021 / mo</span>
            </div>
            <div className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800 flex justify-between items-center">
              <span className="text-slate-300">Idle Compute Waste Ratio:</span>
              <span className="font-bold text-amber-400">14.9% (Actionable)</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
