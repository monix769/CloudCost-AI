import React, { useState } from 'react';
import RecentBillingTable from '../components/RecentBillingTable';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import { FileText, Download, CheckCircle, CreditCard, ArrowDownRight, Layers } from 'lucide-react';

export default function BillingManagementPage() {
  const [downloading, setDownloading] = useState(false);

  const invoices = [
    { id: 'INV-2026-11', period: 'Nov 1 - Nov 30, 2026 (MTD)', amount: 42850.20, status: 'PROCESSING', dueDate: 'Dec 05, 2026' },
    { id: 'INV-2026-10', period: 'Oct 1 - Oct 31, 2026', amount: 45600.00, status: 'PAID', dueDate: 'Nov 05, 2026' },
    { id: 'INV-2026-09', period: 'Sep 1 - Sep 30, 2026', amount: 43300.00, status: 'PAID', dueDate: 'Oct 05, 2026' },
  ];

  const handleDownloadCsv = () => {
    setDownloading(true);
    setTimeout(() => {
      setDownloading(false);
      alert('Billing CSV Export: Sample invoice dataset downloaded successfully.');
    }, 1000);
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <DemoDataDisclaimer />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>Invoices &amp; Billing Records</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Aggregated monthly statements and itemized cloud infrastructure billing logs
          </p>
        </div>

        <button
          onClick={handleDownloadCsv}
          disabled={downloading}
          className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all self-start sm:self-auto"
        >
          <Download className={`w-4 h-4 ${downloading ? 'animate-bounce' : ''}`} />
          <span>{downloading ? 'Generating CSV...' : 'Download Billing Export (CSV)'}</span>
        </button>
      </div>

      {/* Invoice Statements Strip */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {invoices.map((inv) => (
          <div key={inv.id} className="glass-panel rounded-2xl p-5 border border-white/[0.08] space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-mono font-bold text-blue-400">{inv.id}</span>
              <span
                className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
                  inv.status === 'PAID'
                    ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                    : 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                }`}
              >
                {inv.status}
              </span>
            </div>

            <div>
              <p className="text-2xl font-extrabold font-mono text-white">
                ${inv.amount.toLocaleString(undefined, { minimumFractionDigits: 2 })}
              </p>
              <p className="text-xs text-slate-400 mt-0.5">{inv.period}</p>
            </div>

            <div className="pt-2 border-t border-slate-800/80 flex justify-between items-center text-[11px] text-slate-400 font-mono">
              <span>Due: {inv.dueDate}</span>
              <button className="text-blue-400 hover:underline">View PDF</button>
            </div>
          </div>
        ))}
      </div>

      {/* Itemized Billing Table */}
      <RecentBillingTable />
    </div>
  );
}
