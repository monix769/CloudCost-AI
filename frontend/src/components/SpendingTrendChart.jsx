import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import '../utils/chartConfig';
import { commonChartOptions } from '../utils/chartConfig';
import { monthlySpendTrends as fallbackMock } from '../utils/mockFinOpsData';
import costAnalyticsService from '../services/costAnalyticsService';
import { formatCurrency } from '../utils/formatters';
import { TrendingUp, Calendar, RefreshCw, Download } from 'lucide-react';

export default function SpendingTrendChart({
  initialGranularity = 'MONTHLY',
  currency = 'USD',
  provider = null,
  showControls = true,
}) {
  const [granularity, setGranularity] = useState(initialGranularity);
  const [trendsSeries, setTrendsSeries] = useState(null);
  const [loading, setLoading] = useState(true);
  const [useLive, setUseLive] = useState(false);

  useEffect(() => {
    async function loadTrends() {
      try {
        setLoading(true);
        const data = await costAnalyticsService.getSpendingTrends({
          granularity,
          currency,
          provider: provider === 'ALL' ? null : provider,
        });

        if (data && data.points && data.points.length > 0) {
          setTrendsSeries(data);
          setUseLive(true);
        } else {
          // If no custom series points, attempt fallback to legacy endpoint
          const legacy = await costAnalyticsService.getCostTrends();
          if (legacy && legacy.length > 0) {
            setTrendsSeries({
              points: legacy.map((l) => ({
                period: l.period,
                totalCost: l.totalCost,
                awsCost: l.awsCost,
                azureCost: l.azureCost,
                gcpCost: l.gcpCost,
                percentageChange: 0,
              })),
            });
            setUseLive(true);
          } else {
            setUseLive(false);
          }
        }
      } catch (err) {
        console.warn('Failed to load live spending trends, falling back to cached baseline:', err);
        setUseLive(false);
      } finally {
        setLoading(false);
      }
    }
    loadTrends();
  }, [granularity, currency, provider]);

  const points = trendsSeries?.points || [];

  const chartData = useLive && points.length > 0
    ? {
        labels: points.map((p) => p.period),
        datasets: [
          {
            label: 'Total Spend',
            data: points.map((p) => p.totalCost),
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            fill: true,
            tension: 0.3,
            borderWidth: 2,
          },
          {
            label: 'AWS',
            data: points.map((p) => p.awsCost || 0),
            borderColor: '#ff9900',
            backgroundColor: 'transparent',
            borderWidth: 1.5,
            tension: 0.3,
          },
          {
            label: 'Azure',
            data: points.map((p) => p.azureCost || 0),
            borderColor: '#6366f1',
            backgroundColor: 'transparent',
            borderWidth: 1.5,
            tension: 0.3,
          },
          {
            label: 'GCP',
            data: points.map((p) => p.gcpCost || 0),
            borderColor: '#0ea5e9',
            backgroundColor: 'transparent',
            borderWidth: 1.5,
            tension: 0.3,
          },
        ],
      }
    : fallbackMock;

  const totalAws = useLive && points.length > 0
    ? points.reduce((sum, t) => sum + Number(t.awsCost || 0), 0)
    : 23140;
  const totalGcp = useLive && points.length > 0
    ? points.reduce((sum, t) => sum + Number(t.gcpCost || 0), 0)
    : 12410;
  const totalAzure = useLive && points.length > 0
    ? points.reduce((sum, t) => sum + Number(t.azureCost || 0), 0)
    : 7300;

  const options = {
    ...commonChartOptions,
    plugins: {
      ...commonChartOptions.plugins,
      legend: {
        ...commonChartOptions.plugins.legend,
        position: 'top',
        align: 'end',
      },
      tooltip: {
        ...commonChartOptions.plugins.tooltip,
        callbacks: {
          label: (context) => {
            const val = context.parsed.y;
            return ` ${context.dataset.label}: ${formatCurrency(val, currency)}`;
          },
        },
      },
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated flex flex-col justify-between h-full space-y-3">
      {/* Chart Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-3 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <TrendingUp className="w-5 h-5 text-blue-400" />
            <h3 className="font-bold text-white text-base">Cloud Spending Trends</h3>
            <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold border ${
              useLive
                ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20'
                : 'bg-blue-500/10 text-blue-300 border-blue-500/20'
            }`}>
              {useLive ? '● Live Aggregated' : 'Baseline View'}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            Chronological multi-cloud expenditures aggregated across AWS, Azure, and GCP
          </p>
        </div>

        {/* Granularity & Export Controls */}
        {showControls && (
          <div className="flex items-center space-x-2 self-start sm:self-auto">
            <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 text-xs font-mono">
              {[
                { id: 'DAILY', label: 'Day' },
                { id: 'WEEKLY', label: 'Wk' },
                { id: 'MONTHLY', label: 'Mo' },
                { id: 'QUARTERLY', label: 'Qtr' },
              ].map((g) => (
                <button
                  key={g.id}
                  onClick={() => setGranularity(g.id)}
                  className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-all ${
                    granularity === g.id
                      ? 'bg-blue-600 text-white shadow-sm'
                      : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {g.label}
                </button>
              ))}
            </div>

            <button
              onClick={() => costAnalyticsService.downloadExport('spending-trends', { granularity, currency })}
              className="p-1.5 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 transition-colors"
              title="Export trend series CSV"
            >
              <Download className="w-3.5 h-3.5 text-blue-400" />
            </button>
          </div>
        )}
      </div>

      {/* Chart Canvas */}
      <div className="h-72 w-full pt-2 relative">
        {loading ? (
          <div className="h-full w-full flex items-center justify-center text-xs text-slate-500">
            <RefreshCw className="w-5 h-5 animate-spin text-blue-400 mr-2" />
            Loading trend metrics...
          </div>
        ) : (
          <Line data={chartData} options={options} />
        )}
      </div>

      {/* Footer stats summary */}
      <div className="pt-3 border-t border-slate-800/80 grid grid-cols-3 gap-2 text-center text-xs">
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">AWS Spend</p>
          <p className="font-bold text-amber-400 font-mono text-sm">{formatCurrency(totalAws, currency)}</p>
        </div>
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">GCP Spend</p>
          <p className="font-bold text-sky-400 font-mono text-sm">{formatCurrency(totalGcp, currency)}</p>
        </div>
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">Azure Spend</p>
          <p className="font-bold text-indigo-400 font-mono text-sm">{formatCurrency(totalAzure, currency)}</p>
        </div>
      </div>
    </div>
  );
}
