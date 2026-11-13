import React, { useState } from 'react';
import apiClient, { API_BASE_URL } from '../services/api';
import { Send, CheckCircle2, AlertCircle, Clock, Copy, Check } from 'lucide-react';

export default function EndpointTester() {
  const [selectedEndpoint, setSelectedEndpoint] = useState('/');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState(null);
  const [latency, setLatency] = useState(null);
  const [error, setError] = useState(null);
  const [copied, setCopied] = useState(false);

  const endpoints = [
    { path: '/', label: 'GET /', description: 'Platform discovery, metadata, and root API links' },
    { path: '/api/health', label: 'GET /api/health', description: 'Operational health check & MySQL probe' },
    { path: '/api/status', label: 'GET /api/status', description: 'Runtime diagnostics, memory, & modules' },
  ];

  const handleTest = async () => {
    setLoading(true);
    setError(null);
    setResponse(null);
    const start = performance.now();

    try {
      const res = await apiClient.get(selectedEndpoint);
      const end = performance.now();
      setLatency(Math.round(end - start));
      setResponse({
        status: res.status,
        statusText: res.statusText,
        headers: res.headers,
        data: res.data,
      });
    } catch (err) {
      const end = performance.now();
      setLatency(Math.round(end - start));
      setError({
        status: err.status || 500,
        message: err.message,
        data: err.data,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = () => {
    const textToCopy = JSON.stringify(response?.data || error?.data || error, null, 2);
    navigator.clipboard.writeText(textToCopy);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/10 shadow-xl">
      <div className="flex flex-col md:flex-row md:items-center justify-between pb-6 border-b border-slate-800 gap-4">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center space-x-2">
            <span>Interactive REST API Explorer</span>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-teal-500/10 text-teal-300 border border-teal-500/20">
              Live Verified
            </span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Execute real HTTP requests directly against the Spring Boot 3 backend on <span className="text-teal-400 font-mono">{API_BASE_URL.replace(/^https?:\/\//, '')}</span>
          </p>
        </div>

        <button
          onClick={handleTest}
          disabled={loading}
          className="flex items-center justify-center space-x-2 px-5 py-2.5 rounded-xl font-semibold text-xs text-slate-950 bg-gradient-to-r from-teal-400 to-cyan-400 hover:from-teal-300 hover:to-cyan-300 transition-all shadow-lg shadow-teal-500/20 active:scale-95 disabled:opacity-50"
        >
          <Send className={`w-3.5 h-3.5 ${loading ? 'animate-pulse' : ''}`} />
          <span>{loading ? 'Dispatching...' : 'Execute Request'}</span>
        </button>
      </div>

      {/* Endpoint Selector Tabs */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 my-6">
        {endpoints.map((ep) => (
          <button
            key={ep.path}
            onClick={() => {
              setSelectedEndpoint(ep.path);
              setResponse(null);
              setError(null);
            }}
            className={`text-left p-3.5 rounded-xl border transition-all ${
              selectedEndpoint === ep.path
                ? 'bg-teal-500/15 border-teal-500/40 text-teal-300 shadow-sm'
                : 'bg-slate-900/40 border-slate-800 text-slate-400 hover:border-slate-700 hover:text-slate-300'
            }`}
          >
            <div className="flex items-center justify-between mb-1">
              <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-slate-950/80 text-teal-300 border border-teal-500/20">
                {ep.label}
              </span>
            </div>
            <p className="text-[11px] text-slate-400 line-clamp-2 mt-1">{ep.description}</p>
          </button>
        ))}
      </div>

      {/* Response Box */}
      <div className="rounded-xl border border-slate-800 bg-[#070b12] p-4 font-mono text-xs overflow-hidden">
        <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-800/80 text-[11px]">
          <div className="flex items-center space-x-3">
            <span className="text-slate-400">Request:</span>
            <span className="text-teal-400 font-bold">GET {selectedEndpoint}</span>
          </div>

          <div className="flex items-center space-x-4">
            {latency !== null && (
              <span className="flex items-center space-x-1 text-slate-400">
                <Clock className="w-3 h-3 text-slate-400" />
                <span>{latency} ms</span>
              </span>
            )}

            {response && (
              <span className="flex items-center space-x-1 px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                <CheckCircle2 className="w-3 h-3" />
                <span>200 OK</span>
              </span>
            )}

            {error && (
              <span className="flex items-center space-x-1 px-2 py-0.5 rounded bg-rose-500/20 text-rose-300 border border-rose-500/30">
                <AlertCircle className="w-3 h-3" />
                <span>{error.status} Error</span>
              </span>
            )}

            {(response || error) && (
              <button
                onClick={handleCopy}
                className="p-1 rounded hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
                title="Copy JSON Response"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              </button>
            )}
          </div>
        </div>

        {/* Content Viewer */}
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center text-slate-400 space-y-2">
            <div className="w-6 h-6 border-2 border-teal-500 border-t-transparent rounded-full animate-spin"></div>
            <p className="text-xs">Querying Spring Boot REST controller...</p>
          </div>
        ) : response ? (
          <pre className="max-h-80 overflow-y-auto text-emerald-400/90 text-[12px] leading-relaxed scrollbar-thin">
            {JSON.stringify(response.data, null, 2)}
          </pre>
        ) : error ? (
          <div className="py-6 space-y-2">
            <p className="text-rose-400 font-semibold">{error.message}</p>
            {error.data && (
              <pre className="text-rose-300/80 text-[12px]">
                {JSON.stringify(error.data, null, 2)}
              </pre>
            )}
            <p className="text-[11px] text-slate-400 mt-2">
              Tip: Ensure the Spring Boot backend is actively running on port 8080 with MySQL online.
            </p>
          </div>
        ) : (
          <div className="py-10 text-center text-slate-400">
            <p>Click &quot;Execute Request&quot; above to trigger live REST query.</p>
          </div>
        )}
      </div>
    </div>
  );
}
