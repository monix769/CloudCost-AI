import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import '../utils/chartConfig';
import { commonChartOptions } from '../utils/chartConfig';
import { monthlySpendTrends as fallbackMock } from '../utils/mockFinOpsData';
import costAnalyticsService from '../services/costAnalyticsService';
import { TrendingUp, Calendar, RefreshCw } from 'lucide-react';

export default function SpendingTrendChart() {
  const [trends, setTrends] = useState([]);
  const [loading, setLoading] = useState(true);
  const [useLive, setUseLive] = useState(false);

  useEffect(() => {
    async function loadTrends() {
      try {
        setLoading(true);
        const data = await costAnalyticsService.getCostTrends();
        if (data && data.length > 0) {
          setTrends(data);
          setUseLive(true);
        } else {
          setUseLive(false);
        }
      } catch (err) {
        console.warn('Failed to load live cost trends, falling back to cached baseline:', err);
        setUseLive(false);
      } finally {
        setLoading(false);
      }
    }
    loadTrends();
  }, []);

  const chartData = useLive
    ? {
        labels: trends.map((t) => t.period),
        datasets: [
          {
            label: 'AWS',
            data: trends.map((t) => t.awsCost),
            borderColor: '#ff9900',
            backgroundColor: 'rgba(255, 153, 0, 0.1)',
            fill: true,
            tension: 0.35,
          },
          {
            label: 'GCP',
            data: trends.map((t) => t.gcpCost),
            borderColor: '#0ea5e9',
            backgroundColor: 'rgba(14, 165, 233, 0.1)',
            fill: true,
            tension: 0.35,
          },
          {
            label: 'Azure',
            data: trends.map((t) => t.azureCost),
            borderColor: '#6366f1',
            backgroundColor: 'rgba(99, 102, 241, 0.1)',
            fill: true,
            tension: 0.35,
          },
        ],
      }
    : fallbackMock;

  const totalAws = useLive
    ? trends.reduce((sum, t) => sum + Number(t.awsCost || 0), 0)
    : 23140;
  const totalGcp = useLive
    ? trends.reduce((sum, t) => sum + Number(t.gcpCost || 0), 0)
    : 12410;
  const totalAzure = useLive
    ? trends.reduce((sum, t) => sum + Number(t.azureCost || 0), 0)
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
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated flex flex-col justify-between h-full">
      {/* Chart Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-slate-800 gap-3">
        <div>
          <div className="flex items-center space-x-2">
            <h3 className="font-bold text-white text-base">Monthly Spending Trends</h3>
            <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold border ${
              useLive
                ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/20'
                : 'bg-blue-500/10 text-blue-300 border-blue-500/20'
            }`}>
              {useLive ? '● Live Aggregated' : 'Demonstration'}
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-0.5">
            {useLive ? 'Persisted MySQL multi-cloud time-series expenditure' : 'Historical infrastructure expenditures aggregated across AWS, GCP, and Azure'}
          </p>
        </div>
      </div>

      {/* Chart Canvas */}
      <div className="h-72 w-full pt-4">
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
      <div className="pt-4 mt-2 border-t border-slate-800/80 grid grid-cols-3 gap-2 text-center text-xs">
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">AWS Spend</p>
          <p className="font-bold text-amber-400 font-mono text-sm">${totalAws.toLocaleString(undefined, { maximumFractionDigits: 0 })}</p>
        </div>
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">GCP Spend</p>
          <p className="font-bold text-sky-400 font-mono text-sm">${totalGcp.toLocaleString(undefined, { maximumFractionDigits: 0 })}</p>
        </div>
        <div>
          <p className="text-[10px] text-slate-400 uppercase font-mono">Azure Spend</p>
          <p className="font-bold text-indigo-400 font-mono text-sm">${totalAzure.toLocaleString(undefined, { maximumFractionDigits: 0 })}</p>
        </div>
      </div>
    </div>
  );
}
