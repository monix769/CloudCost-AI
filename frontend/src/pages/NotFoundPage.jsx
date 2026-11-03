import React from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, CloudOff } from 'lucide-react';

export default function NotFoundPage() {
  return (
    <div className="py-24 flex flex-col items-center justify-center text-center space-y-4 animate-fadeIn">
      <div className="p-4 rounded-2xl bg-slate-900 border border-slate-800 text-teal-400">
        <CloudOff className="w-12 h-12" />
      </div>
      <h1 className="text-4xl font-extrabold text-white">404 - Endpoint Not Found</h1>
      <p className="text-sm text-slate-400 max-w-md">
        The requested page or route does not exist within the CloudCost AI platform shell.
      </p>
      <Link
        to="/"
        className="inline-flex items-center space-x-2 px-5 py-2.5 rounded-xl font-semibold text-xs text-slate-950 bg-teal-400 hover:bg-teal-300 transition-all shadow-lg shadow-teal-500/20"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Return to Platform Architecture</span>
      </Link>
    </div>
  );
}
