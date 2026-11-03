import React from 'react';
import { Info, Sparkles, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function DemoDataDisclaimer() {
  return (
    <div className="rounded-xl border border-blue-500/20 bg-blue-950/20 px-4 py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
      <div className="flex items-center space-x-2.5 text-blue-200">
        <Sparkles className="w-4 h-4 text-blue-400 shrink-0" />
        <span>
          <strong className="text-white font-semibold">Demonstration Mode:</strong> Showcasing multi-cloud FinOps analytics for <span className="text-blue-300 font-medium">Acme Cloud Corp</span>. Link your AWS, GCP, or Azure accounts in Milestone 3 to stream real-time billing records.
        </span>
      </div>

      <Link
        to="/cloud-accounts"
        className="inline-flex items-center space-x-1 font-semibold text-blue-400 hover:text-blue-300 underline underline-offset-2 shrink-0 text-[11px]"
      >
        <span>Manage Cloud Accounts</span>
        <ExternalLink className="w-3 h-3" />
      </Link>
    </div>
  );
}
