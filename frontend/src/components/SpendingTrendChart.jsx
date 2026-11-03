import React, { useState } from 'react';
import { Line } from 'react-chartjs-2';
import '../utils/chartConfig';
import { commonChartOptions } from '../utils/chartConfig';
import { monthlySpendTrends } from '../utils/mockFinOpsData';
import { TrendingUp, Calendar } from 'lucide-react';

export default function SpendingTrendChart() {
  const [timeRange, setTimeRange] = useState('6M');

  const options = {
    ...commonChartOptions,
    plugins: {
      ...commonChartOptions.plugins,
      legend: {
        ...commonChartOptions.plugins.legend,
        position: 'top',
        align: 'end',
      },
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated flex flex-col justify-between h-full">
      {/* Chart Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="font-bold text-white text-base">Monthly Spending Trends</h3>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold bg-blue-500/10 text-blue-300 border border-blue-500/20">
              Multi-Cloud
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Historical infrastructure expenditures aggregated across AWS, GCP, and Azure
          </p>
        </div>

        {/* Time-range filter pills */}
        <div className="flex items-center space-x-1 p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs self-start sm:self-auto font-mono">
          {['30D', '3M', '6M'].map((range) => (
            <button
              key={range}
              onClick={() => setTimeRange(range)}
              className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                timeRange === range
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
              }`}
            >
              {range}
            </button>
          ))}
        </div>
      </div>

      {/* Chart Canvas */}
      <div className="h-72 w-full pt-4">
        <Line data={monthlySpendTrends} options={options} />
      </div>

      {/* Footer stats summary */}
      <div className="pt-4 mt-2 border-t border-slate-800/80 grid grid-cols-3 gap-2 text-center text-xs">
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">AWS Spend</p>
          <p className="font-bold text-amber-400 font-mono text-sm">$23,140</p>
        </div>
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">GCP Spend</p>
          <p className="font-bold text-sky-400 font-mono text-sm">$12,410</p>
        </div>
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">Azure Spend</p>
          <p className="font-bold text-indigo-400 font-mono text-sm">$7,300</p>
        </div>
      </div>
    </div>
  );
}
