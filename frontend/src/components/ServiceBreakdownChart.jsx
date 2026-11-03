import React from 'react';
import { Bar } from 'react-chartjs-2';
import '../utils/chartConfig';
import { commonChartOptions } from '../utils/chartConfig';
import { serviceBreakdown } from '../utils/mockFinOpsData';
import { Server } from 'lucide-react';

export default function ServiceBreakdownChart() {
  const options = {
    ...commonChartOptions,
    indexAxis: 'y', // Horizontal bars for clean service names
    plugins: {
      ...commonChartOptions.plugins,
      legend: {
        display: false,
      },
    },
    scales: {
      x: {
        grid: {
          color: 'rgba(255, 255, 255, 0.04)',
          drawBorder: false,
        },
        ticks: {
          color: '#64748b',
          font: { size: 10 },
          callback: function (val) {
            return '$' + (val / 1000).toFixed(0) + 'k';
          },
        },
      },
      y: {
        grid: {
          display: false,
        },
        ticks: {
          color: '#cbd5e1',
          font: { size: 11, family: 'Inter, sans-serif', weight: '500' },
        },
      },
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated flex flex-col justify-between h-full">
      {/* Header */}
      <div className="pb-4 border-b border-slate-800">
        <div className="flex items-center space-x-2">
          <h3 className="font-bold text-white text-base">Top Services Cost Breakdown</h3>
        </div>
        <p className="text-xs text-slate-400 mt-0.5">
          Highest cost-generating cloud services ranked by current month spend
        </p>
      </div>

      {/* Chart Canvas */}
      <div className="h-72 w-full pt-4">
        <Bar data={serviceBreakdown} options={options} />
      </div>

      {/* Footer Info */}
      <div className="pt-3 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
        <span>Highest: <strong className="text-amber-400">Amazon EC2 ($11,240)</strong></span>
        <span>Top 7 represent <strong className="text-slate-200">92%</strong> of compute spend</span>
      </div>
    </div>
  );
}
