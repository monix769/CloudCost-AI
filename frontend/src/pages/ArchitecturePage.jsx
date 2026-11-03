import React from 'react';
import ArchitectureDiagram from '../components/ArchitectureDiagram';
import EndpointTester from '../components/EndpointTester';
import { Layers, ShieldCheck, CheckCircle2, Server, Database, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function ArchitecturePage() {
  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center space-x-2">
            <h2 className="text-xl font-bold text-white">Full-Stack System Architecture</h2>
            <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-teal-500/20 text-teal-300 border border-teal-500/30">
              Milestone 1 Verified
            </span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            4-tier decoupled enterprise architecture spanning React 19, Spring Boot 3.3.5, and MySQL 8
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <a
            href="http://localhost:8080/swagger-ui.html"
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white transition-all shadow-md"
          >
            <span>Open Swagger UI</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>

      {/* Visual System Architecture Diagram */}
      <section>
        <ArchitectureDiagram />
      </section>

      {/* Live Interactive REST API Explorer */}
      <section>
        <EndpointTester />
      </section>
    </div>
  );
}
