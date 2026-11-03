import React, { useState } from 'react';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import { FileBarChart, Download, Calendar, Mail, FileText, CheckCircle, ArrowDown } from 'lucide-react';

export default function ReportsPage() {
  const [downloadingId, setDownloadingId] = useState(null);

  const reports = [
    {
      id: 'rep-1',
      title: 'Executive Multi-Cloud FinOps Summary',
      period: 'October 2026',
      type: 'Executive Brief',
      size: '1.4 MB (PDF)',
      author: 'FinOps AI Engine',
      description: 'Comprehensive analysis of multi-cloud expenditures, MoM variances, budget health, and executive key takeaways.',
    },
    {
      id: 'rep-2',
      title: 'Kubernetes & Container Cost Showback',
      period: 'October 2026',
      type: 'Technical Showback',
      size: '2.8 MB (CSV / PDF)',
      author: 'Container Cost Agent',
      description: 'Namespace-level, pod-level, and cluster-level cost allocation across GKE and EKS production environments.',
    },
    {
      id: 'rep-3',
      title: 'Multi-Cloud Tag Compliance & Governance Audit',
      period: 'Q3 2026',
      type: 'Governance',
      size: '950 KB (PDF)',
      author: 'Policy Sentinel',
      description: 'Audit of untagged or improperly categorized resources across AWS, GCP, and Azure fleets.',
    },
    {
      id: 'rep-4',
      title: 'Unit Economics & COGS Impact Analysis',
      period: 'Q3 2026',
      type: 'Financial Analytics',
      size: '3.1 MB (PDF)',
      author: 'Finance & Analytics',
      description: 'Correlation of cloud infrastructure costs with gross revenue, customer acquisition, and daily active user volume.',
    },
  ];

  const handleDownload = (id) => {
    setDownloadingId(id);
    setTimeout(() => {
      setDownloadingId(null);
      alert('Report exported successfully (Demonstration Mode).');
    }, 1200);
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <DemoDataDisclaimer />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>Executive FinOps Reports &amp; Statements</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Pre-compiled and custom cloud expenditure reports for finance leads, CTOs, and engineering managers
          </p>
        </div>

        <button
          onClick={() => alert('Custom Report Builder simulated (Milestone 2 Demo).')}
          className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all self-start sm:self-auto"
        >
          <FileBarChart className="w-4 h-4" />
          <span>Generate Custom Report</span>
        </button>
      </div>

      {/* Reports Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {reports.map((r) => (
          <div
            key={r.id}
            className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4 flex flex-col justify-between"
          >
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 border border-blue-500/20">
                  {r.type}
                </span>
                <span className="text-xs font-mono text-slate-400">{r.period}</span>
              </div>

              <h3 className="font-bold text-white text-base">{r.title}</h3>
              <p className="text-xs text-slate-300 leading-relaxed">{r.description}</p>
            </div>

            <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between text-xs">
              <div className="text-[11px] font-mono text-slate-400">
                <span>{r.size} • By {r.author}</span>
              </div>

              <div className="flex items-center space-x-2">
                <button
                  onClick={() => handleDownload(r.id)}
                  disabled={downloadingId === r.id}
                  className="px-3 py-1.5 rounded-xl bg-navy-900 hover:bg-navy-800 text-slate-200 border border-slate-700 font-semibold flex items-center space-x-1.5 transition-colors"
                >
                  <Download className={`w-3.5 h-3.5 text-blue-400 ${downloadingId === r.id ? 'animate-bounce' : ''}`} />
                  <span>{downloadingId === r.id ? 'Exporting...' : 'Download'}</span>
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
