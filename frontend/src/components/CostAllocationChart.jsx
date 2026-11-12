import React, { useState, useEffect } from 'react';
import { Bar, Doughnut } from 'react-chartjs-2';
import '../utils/chartConfig';
import { commonChartOptions } from '../utils/chartConfig';
import costAnalyticsService from '../services/costAnalyticsService';
import { formatCurrency } from '../utils/formatters';
import { PieChart, Layers, Globe, Server, UserCheck } from 'lucide-react';

export default function CostAllocationChart({ currency = 'USD' }) {
  const [dimension, setDimension] = useState('SERVICE');
  const [allocation, setAllocation] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadAllocation() {
      try {
        setLoading(true);
        const data = await costAnalyticsService.getCostAllocation({
          dimension,
          currency,
        });
        setAllocation(data);
      } catch (err) {
        console.error('Failed to load cost allocation:', err);
      } finally {
        setLoading(false);
      }
    }
    loadAllocation();
  }, [dimension, currency]);

  const items = allocation?.items || [];
  const topItems = items.slice(0, 7);

  const colors = [
    '#3b82f6', // blue
    '#a855f7', // purple
    '#0ea5e9', // cyan
    '#10b981', // emerald
    '#f59e0b', // amber
    '#ec4899', // pink
    '#6366f1', // indigo
  ];

  const chartData = {
    labels: topItems.map((i) => i.key),
    datasets: [
      {
        data: topItems.map((i) => i.cost),
        backgroundColor: colors.slice(0, topItems.length),
        borderColor: '#0b1120',
        borderWidth: 2,
      },
    ],
  };

  const doughnutOptions = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '68%',
    plugins: {
      legend: {
        position: 'right',
        labels: {
          color: '#cbd5e1',
          boxWidth: 10,
          font: { size: 10, family: 'Inter, sans-serif' },
        },
      },
      tooltip: {
        ...commonChartOptions.plugins.tooltip,
        callbacks: {
          label: (context) => {
            const item = topItems[context.dataIndex];
            return ` ${item.key}: ${formatCurrency(item.cost, currency)} (${item.percentage}%)`;
          },
        },
      },
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated flex flex-col justify-between h-full space-y-4">
      {/* Header & Dimension Toggles */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <Layers className="w-5 h-5 text-blue-400" />
            <h3 className="font-bold text-white text-base">Cost Allocation & Distribution</h3>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Breakdown across cloud architectural dimensions
          </p>
        </div>

        {/* Dimension Selector Pills */}
        <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono self-start sm:self-auto">
          {[
            { id: 'SERVICE', label: 'Service' },
            { id: 'PROVIDER', label: 'Provider' },
            { id: 'ACCOUNT', label: 'Account' },
            { id: 'REGION', label: 'Region' },
          ].map((d) => (
            <button
              key={d.id}
              onClick={() => setDimension(d.id)}
              className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-all ${
                dimension === d.id
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {d.label}
            </button>
          ))}
        </div>
      </div>

      {/* Chart Canvas & Summary Table */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 items-center">
        {/* Doughnut Chart */}
        <div className="h-56 relative flex items-center justify-center">
          {loading ? (
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
          ) : topItems.length === 0 ? (
            <div className="text-xs text-slate-500 font-mono">No allocation data recorded for this dimension.</div>
          ) : (
            <Doughnut data={chartData} options={doughnutOptions} />
          )}
        </div>

        {/* Detailed Item List */}
        <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
          {topItems.map((item, index) => (
            <div key={item.key} className="p-2 rounded-xl bg-navy-900/50 border border-slate-800/80 flex items-center justify-between text-xs">
              <div className="flex items-center space-x-2 truncate">
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: colors[index % colors.length] }}
                />
                <div className="truncate">
                  <div className="font-semibold text-slate-200 truncate">{item.key}</div>
                  {item.subKey && <div className="text-[10px] text-slate-500 font-mono">{item.subKey}</div>}
                </div>
              </div>
              <div className="text-right shrink-0 ml-2">
                <div className="font-mono font-bold text-white">{formatCurrency(item.cost, currency)}</div>
                <div className="text-[10px] text-blue-400 font-mono">{item.percentage}%</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

