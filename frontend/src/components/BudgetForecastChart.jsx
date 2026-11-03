import React from 'react';
import { Bar } from 'react-chartjs-2';
import '../utils/chartConfig';
import { commonChartOptions } from '../utils/chartConfig';
import { budgetForecast } from '../utils/mockFinOpsData';
import { Target, CheckCircle2 } from 'lucide-react';

export default function BudgetForecastChart() {
  const options = {
    ...commonChartOptions,
    plugins: {
      ...commonChartOptions.plugins,
      legend: {
        display: false,
      },
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated flex flex-col justify-between h-full">
      {/* Header */}
      <div className="pb-4 border-b border-slate-800">
        <div className="flex items-center space-x-2">
          <h3 className="font-bold text-white text-base">Budget vs. Forecast Variance</h3>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
            On Track
          </span>
        </div>
        <p className="text-xs text-slate-400 mt-0.5">
          Current month-to-date actuals against AI projected run-rate and total budget limit
        </p>
      </div>

      {/* Chart Canvas */}
      <div className="h-72 w-full pt-4">
        <Bar data={budgetForecast} options={options} />
      </div>

      {/* Footer Metrics */}
      <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs">
        <div className="flex items-center space-x-1.5 text-emerald-400 font-medium">
          <CheckCircle2 className="w-4 h-4" />
          <span>Projected buffer remaining: <strong className="font-mono">$1,800</strong></span>
        </div>
        <span className="text-slate-400 text-[11px] font-mono">Limit: $50,000.00</span>
      </div>
    </div>
  );
}
