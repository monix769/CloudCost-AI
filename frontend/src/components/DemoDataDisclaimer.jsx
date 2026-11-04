import React from 'react';
import { Info, Sparkles, ExternalLink, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function DemoDataDisclaimer() {
  return (
    <div className="rounded-xl border border-blue-500/20 bg-blue-950/20 px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
      <div className="flex items-center space-x-2.5 text-blue-200">
        <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
        <span>
          <strong className="text-white font-semibold">Provenance &amp; Multi-Cloud Ingestion:</strong> Billing records strictly distinguish <span className="text-emerald-300 font-mono font-bold">REAL_PROVIDER</span> (live authenticated API), <span className="text-amber-300 font-mono font-bold">MOCK_PROVIDER</span> (sandbox verification pending), and <span className="text-purple-300 font-mono font-bold">CSV_UPLOAD</span>. Currency consistency enforced across all analytics.
        </span>
      </div>

      <Link
        to="/cloud-accounts"
        className="inline-flex items-center space-x-1 font-semibold text-blue-400 hover:text-blue-300 underline underline-offset-2 shrink-0 text-[11px]"
      >
        <span>Manage Cloud Ingestion</span>
        <ExternalLink className="w-3 h-3" />
      </Link>
    </div>
  );
}
