import React, { useState, useEffect } from 'react';
import { Line } from 'react-chartjs-2';
import '../utils/chartConfig';
import { commonChartOptions } from '../utils/chartConfig';
import costAnalyticsService from '../services/costAnalyticsService';
import { formatCurrency } from '../utils/formatters';
import { Sparkles, TrendingUp, Info, Sliders, Calendar, Download } from 'lucide-react';

export default function CostForecastingChart({ currency = 'USD' }) {
  const [horizon, setHorizon] = useState('30D');
  const [method, setMethod] = useState('EXPONENTIAL_SMOOTHING');
  const [alpha, setAlpha] = useState(0.3);
  const [forecastResult, setForecastResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function fetchForecast() {
      try {
        setLoading(true);
        setError(null);
        const data = await costAnalyticsService.getForecast({
          horizon,
          method,
          alpha,
          currency,
        });
        setForecastResult(data);
      } catch (err) {
        console.error('Failed to load cost forecast:', err);
        setError('Unable to load forecast model from billing records.');
      } finally {
        setLoading(false);
      }
    }
    fetchForecast();
  }, [horizon, method, alpha, currency]);

  const historical = forecastResult?.historicalData || [];
  const forecasted = forecastResult?.forecastData || [];

  const labels = [
    ...historical.map((h) => h.date),
    ...forecasted.map((f) => f.date),
  ];

  const historicalSeries = [
    ...historical.map((h) => h.cost),
    ...forecasted.map(() => null),
  ];

  const bridgePoint = historical.length > 0 ? historical[historical.length - 1].cost : null;
  const forecastSeries = [
    ...historical.slice(0, -1).map(() => null),
    ...(bridgePoint !== null ? [bridgePoint] : []),
    ...forecasted.map((f) => f.forecastedCost),
  ];

  const upperBand = [
    ...historical.slice(0, -1).map(() => null),
    ...(bridgePoint !== null ? [bridgePoint] : []),
    ...forecasted.map((f) => f.upperBound),
  ];

  const lowerBand = [
    ...historical.slice(0, -1).map(() => null),
    ...(bridgePoint !== null ? [bridgePoint] : []),
    ...forecasted.map((f) => f.lowerBound),
  ];

  const chartData = {
    labels,
    datasets: [
      {
        label: 'Historical Actuals',
        data: historicalSeries,
        borderColor: '#3b82f6',
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        borderWidth: 2,
        pointRadius: historical.length > 30 ? 0 : 2,
        tension: 0.2,
      },
      {
        label: `Forecast (${method === 'EXPONENTIAL_SMOOTHING' ? 'SES' : 'MA'})`,
        data: forecastSeries,
        borderColor: '#a855f7',
        borderDash: [5, 5],
        borderWidth: 2.5,
        pointRadius: 3,
        pointBackgroundColor: '#a855f7',
        tension: 0.2,
      },
      {
        label: 'Confidence Upper (95%)',
        data: upperBand,
        borderColor: 'rgba(168, 85, 247, 0.3)',
        borderWidth: 1,
        pointRadius: 0,
        fill: '+1',
        backgroundColor: 'rgba(168, 85, 247, 0.1)',
      },
      {
        label: 'Confidence Lower (95%)',
        data: lowerBand,
        borderColor: 'rgba(168, 85, 247, 0.3)',
        borderWidth: 1,
        pointRadius: 0,
        fill: false,
      },
    ],
  };

  const chartOptions = {
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
            if (val === null || val === undefined) return null;
            return `${context.dataset.label}: ${formatCurrency(val, currency)}`;
          },
        },
      },
    },
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
      {/* Header & Controls */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center space-x-2">
            <Sparkles className="w-5 h-5 text-purple-400" />
            <h3 className="font-bold text-white text-base">Statistical Cost Forecasting</h3>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold border bg-purple-500/10 text-purple-300 border-purple-500/20">
              Java Time-Series Model
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Statistical projection with 95% confidence interval based on historical daily billing patterns
          </p>
        </div>

        {/* Model Controls */}
        <div className="flex flex-wrap items-center gap-2 text-xs">
          {/* Horizon Selector */}
          <div className="flex items-center p-1 rounded-xl bg-navy-900 border border-slate-800 font-mono">
            {['7D', '30D', '90D'].map((h) => (
              <button
                key={h}
                onClick={() => setHorizon(h)}
                className={`px-2.5 py-1 rounded-lg text-xs font-semibold transition-all ${
                  horizon === h
                    ? 'bg-purple-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {h}
              </button>
            ))}
          </div>

          {/* Method Selector */}
          <select
            value={method}
            onChange={(e) => setMethod(e.target.value)}
            className="bg-navy-900 border border-slate-800 rounded-xl px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-purple-500 font-mono"
          >
            <option value="EXPONENTIAL_SMOOTHING">Exp. Smoothing (SES)</option>
            <option value="MOVING_AVERAGE">Moving Average (MA)</option>
          </select>

          {/* Alpha Slider (for Exp Smoothing) */}
          {method === 'EXPONENTIAL_SMOOTHING' && (
            <div className="flex items-center space-x-1.5 bg-navy-900 border border-slate-800 rounded-xl px-2.5 py-1 text-slate-400 font-mono">
              <span className="text-[11px]">α: {alpha}</span>
              <input
                type="range"
                min="0.1"
                max="0.9"
                step="0.1"
                value={alpha}
                onChange={(e) => setAlpha(parseFloat(e.target.value))}
                className="w-16 accent-purple-500 cursor-pointer h-1"
                title="Smoothing factor: higher values weight recent spend more heavily"
              />
            </div>
          )}

          <button
            onClick={() => costAnalyticsService.downloadExport('forecast', { horizon, method, alpha, currency })}
            className="inline-flex items-center space-x-1 px-2.5 py-1.5 rounded-xl text-xs font-semibold bg-navy-900 hover:bg-navy-800 text-slate-300 border border-slate-700 transition-colors"
            title="Export forecast dataset as CSV"
          >
            <Download className="w-3.5 h-3.5 text-purple-400" />
            <span>CSV</span>
          </button>
        </div>
      </div>

      {/* Forecast Metric Cards */}
      {forecastResult && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800">
            <span className="text-[11px] font-mono uppercase text-slate-400 font-semibold">Projected Horizon Total</span>
            <div className="text-xl font-bold font-mono text-purple-300 mt-0.5">
              {formatCurrency(forecastResult.estimatedTotalForecastCost, currency)}
            </div>
            <span className="text-[10px] text-slate-500">Across next {forecastResult.horizon}</span>
          </div>

          <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800">
            <span className="text-[11px] font-mono uppercase text-slate-400 font-semibold">Mean Projected Daily</span>
            <div className="text-xl font-bold font-mono text-white mt-0.5">
              {formatCurrency(forecastResult.averageDailyForecastCost, currency)}
            </div>
            <span className="text-[10px] text-slate-500">Run rate estimation</span>
          </div>

          <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800">
            <span className="text-[11px] font-mono uppercase text-slate-400 font-semibold">Model Confidence</span>
            <div className="text-xs font-semibold text-slate-300 mt-1 line-clamp-1">
              {forecastResult.confidenceLevel}
            </div>
            <span className="text-[10px] text-emerald-400">Statistically derived bands</span>
          </div>
        </div>
      )}

      {/* Chart Canvas */}
      <div className="h-72 w-full relative">
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-navy-900/40 rounded-xl">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-500"></div>
          </div>
        ) : error ? (
          <div className="absolute inset-0 flex items-center justify-center text-xs text-rose-400 font-mono">
            {error}
          </div>
        ) : labels.length === 0 ? (
          <div className="absolute inset-0 flex items-center justify-center text-xs text-slate-500 font-mono">
            No historical daily billing data available to generate forecast.
          </div>
        ) : (
          <Line data={chartData} options={chartOptions} />
        )}
      </div>

      {/* Disclaimer / Model Limitations Footer */}
      {forecastResult && (
        <div className="flex items-start space-x-2 p-2.5 rounded-xl bg-navy-900/40 border border-slate-800/60 text-[11px] text-slate-400">
          <Info className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
          <div>
            <span className="font-semibold text-slate-300">Methodology Notice: </span>
            <span>{forecastResult.limitations}</span>
          </div>
        </div>
      )}
    </div>
  );
}

