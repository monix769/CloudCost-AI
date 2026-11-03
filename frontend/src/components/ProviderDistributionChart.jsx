import React from 'react';
import { Doughnut } from 'react-chartjs-2';
import '../utils/chartConfig';
import { providerDistribution } from '../utils/mockFinOpsData';
import { PieChart } from 'lucide-react';

export default function ProviderDistributionChart() {
  const options = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          color: '#cbd5e1',
          boxWidth: 10,
          boxHeight: 10,
          borderRadius: 2,
          usePointStyle: true,
          padding: 16,
          font: {
            family: 'Inter, sans-serif',
            size: 11,
            weight: '500',
          },
        },
      },
      tooltip: {
        backgroundColor: '#0b1120',
        titleColor: '#f8fafc',
        bodyColor: '#93c5fd',
        borderColor: 'rgba(59, 130, 246, 0.3)',
        borderWidth: 1,
        padding: 10,
        callbacks: {
          label: function (context) {
            const value = context.parsed;
            const formatted = new Intl.NumberFormat('en-US', {
              style: 'currency',
              currency: 'USD',
              maximumFractionDigits: 0,
            }).format(value);
            return ` ${context.label}: ${formatted}`;
          },
        },
      },
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated flex flex-col justify-between h-full">
      {/* Header */}
      <div className="pb-4 border-b border-slate-800">
        <div className="flex items-center space-x-2">
          <h3 className="font-bold text-white text-base">Cloud Provider Distribution</h3>
        </div>
        <p className="text-xs text-slate-400 mt-0.5">
          Proportional spending share across active cloud platforms
        </p>
      </div>

      {/* Doughnut Container with Center Readout */}
      <div className="relative h-64 w-full flex items-center justify-center my-2">
        <Doughnut data={providerDistribution} options={options} />

        {/* Center Text Readout */}
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none pb-8">
          <span className="text-[10px] uppercase font-mono font-bold tracking-wider text-slate-400">
            Total Spend
          </span>
          <span className="text-xl font-extrabold text-white font-mono">
            $42,850
          </span>
          <span className="text-[10px] text-emerald-400 font-semibold font-mono">
            3 Providers Active
          </span>
        </div>
      </div>

      {/* Provider Summary List */}
      <div className="pt-3 border-t border-slate-800/80 space-y-2 text-xs">
        <div className="flex items-center justify-between">
          <span className="flex items-center space-x-2 text-slate-300">
            <span className="w-2.5 h-2.5 rounded-full bg-[#ff9900]"></span>
            <span>Amazon Web Services</span>
          </span>
          <span className="font-mono font-bold text-white">$23,140 (54%)</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="flex items-center space-x-2 text-slate-300">
            <span className="w-2.5 h-2.5 rounded-full bg-[#0ea5e9]"></span>
            <span>Google Cloud Platform</span>
          </span>
          <span className="font-mono font-bold text-white">$12,410 (29%)</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="flex items-center space-x-2 text-slate-300">
            <span className="w-2.5 h-2.5 rounded-full bg-[#6366f1]"></span>
            <span>Microsoft Azure</span>
          </span>
          <span className="font-mono font-bold text-white">$7,300 (17%)</span>
        </div>
      </div>
    </div>
  );
}
