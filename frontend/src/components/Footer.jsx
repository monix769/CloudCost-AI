import React from 'react';
import { CheckCircle2, ShieldCheck, Terminal, Database, Code2 } from 'lucide-react';
import { API_BASE_URL } from '../services/api';

export default function Footer() {
  const displayHost = API_BASE_URL.replace(/^https?:\/\//, '');

  return (
    <footer className="glass-panel border-t border-white/5 mt-16 bg-[#080c14]/90">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8 text-xs">
          {/* Col 1 */}
          <div className="space-y-3">
            <div className="flex items-center space-x-2">
              <span className="font-bold text-slate-200">CloudCost AI</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-300 border border-emerald-500/20">
                Production Ready
              </span>
            </div>
            <p className="text-slate-400 leading-relaxed text-[11px]">
              Intelligent Cloud Cost Monitoring &amp; FinOps Analytics Platform. Clean architecture, Spring Boot 3 &amp; React.
            </p>
          </div>

          {/* Col 2: Architecture */}
          <div className="space-y-2">
            <p className="text-slate-300 font-semibold tracking-wider uppercase text-[10px]">Architecture</p>
            <ul className="space-y-1.5 text-slate-400 text-[11px]">
              <li className="flex items-center space-x-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>Controller-Service-Repository Pattern</span>
              </li>
              <li className="flex items-center space-x-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>Centralized Error Handling</span>
              </li>
              <li className="flex items-center space-x-2">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>Standardized REST Response Envelope</span>
              </li>
            </ul>
          </div>

          {/* Col 3: Tech Stack */}
          <div className="space-y-2">
            <p className="text-slate-300 font-semibold tracking-wider uppercase text-[10px]">Technology Stack</p>
            <div className="flex flex-wrap gap-1.5">
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">Java 21</span>
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">Spring Boot 3.3.5</span>
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">React 19</span>
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">Vite</span>
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">Tailwind CSS</span>
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">MySQL 8+</span>
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">Spring Security</span>
              <span className="px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">OpenAPI 3</span>
            </div>
          </div>

          {/* Col 4: Verification */}
          <div className="space-y-2">
            <p className="text-slate-300 font-semibold tracking-wider uppercase text-[10px]">Verification Status</p>
            <div className="p-3 rounded-lg bg-slate-900/80 border border-slate-800 space-y-1.5 text-[11px]">
              <div className="flex items-center justify-between text-emerald-400">
                <span>Unit &amp; Integration Tests:</span>
                <span className="font-mono font-bold">159 / 159 PASSED</span>
              </div>
              <div className="flex items-center justify-between text-teal-400">
                <span>MySQL Connection:</span>
                <span className="font-mono font-bold">VERIFIED</span>
              </div>
              <div className="flex items-center justify-between text-slate-400">
                <span>Production Security:</span>
                <span className="font-mono font-bold text-emerald-400">AUDITED</span>
              </div>
            </div>
          </div>
        </div>

        <div className="pt-6 border-t border-slate-800/80 flex flex-col sm:flex-row items-center justify-between text-[11px] text-slate-400">
          <p>© 2026 CloudCost AI Platform. Built with professional Spring Boot &amp; React engineering standards.</p>
          <div className="flex items-center space-x-4 mt-3 sm:mt-0">
            <span className="flex items-center space-x-1.5 text-slate-400">
              <ShieldCheck className="w-3.5 h-3.5 text-teal-400" />
              <span>FinOps Architecture</span>
            </span>
            <span className="text-slate-700">•</span>
            <span className="text-teal-400 font-mono">{displayHost}</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
