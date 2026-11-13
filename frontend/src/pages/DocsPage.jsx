import React from 'react';
import { ExternalLink, BookOpen, Code, CheckCircle, Shield } from 'lucide-react';
import { API_BASE_URL } from '../services/api';

export default function DocsPage() {
  const endpoints = [
    {
      method: 'GET',
      path: '/',
      summary: 'Root Platform Discovery',
      description: 'Provides platform metadata, versioning, links to documentation and primary REST endpoints.',
      responseExample: {
        success: true,
        message: 'Welcome to CloudCost AI FinOps Platform',
        timestamp: '2026-11-03T15:00:00Z',
        data: {
          platform: 'CloudCost AI',
          title: 'Intelligent Cloud Cost Monitoring & FinOps Analytics Platform',
          version: '1.0.0-SNAPSHOT',
          status: 'ONLINE',
          endpoints: {
            root: 'GET /',
            health: 'GET /api/health',
            status: 'GET /api/status'
          }
        }
      }
    },
    {
      method: 'GET',
      path: '/api/health',
      summary: 'Application & Database Health Probe',
      description: 'Liveness and readiness probe for application runtime, MySQL database connection, and storage.',
      responseExample: {
        success: true,
        message: 'System operational',
        timestamp: '2026-11-03T15:00:00Z',
        data: {
          status: 'UP',
          database: 'CONNECTED',
          version: '1.0.0-SNAPSHOT',
          uptimeSeconds: 120,
          components: {
            database: {
              status: 'UP',
              databaseProduct: 'MySQL',
              databaseVersion: '8.0.35',
              pingLatencyMs: 4
            }
          }
        }
      }
    },
    {
      method: 'GET',
      path: '/api/status',
      summary: 'Detailed Architectural & System Status',
      description: 'Detailed platform status including JVM memory statistics, active profiles, and loaded modules.',
      responseExample: {
        success: true,
        message: 'Status retrieved successfully',
        timestamp: '2026-11-03T15:00:00Z',
        data: {
          application: 'CloudCost AI - FinOps Analytics Platform',
          environment: 'development',
          activeProfiles: ['dev'],
          javaVersion: '21.0.6',
          springBootVersion: '3.3.5',
          securityMode: 'STATELESS_PERMIT_ALL_M1'
        }
      }
    }
  ];

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white flex items-center space-x-3">
            <span>REST API Documentation</span>
            <span className="text-xs px-2.5 py-0.5 rounded-full font-bold bg-teal-500/20 text-teal-300 border border-teal-500/30">
              OpenAPI 3 / Swagger
            </span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Standardized OpenAPI 3 specifications generated automatically by Springdoc and accessible via Swagger UI.
          </p>
        </div>

        <div className="flex items-center space-x-3 self-start sm:self-auto">
          <a
            href={`${API_BASE_URL}/swagger-ui.html`}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-gradient-to-r from-teal-500 to-cyan-500 text-slate-950 font-bold hover:from-teal-400 hover:to-cyan-400 transition-all shadow-lg shadow-teal-500/20"
          >
            <BookOpen className="w-3.5 h-3.5" />
            <span>Open Swagger UI</span>
            <ExternalLink className="w-3 h-3 ml-1" />
          </a>

          <a
            href={`${API_BASE_URL}/v3/api-docs`}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-all"
          >
            <Code className="w-3.5 h-3.5" />
            <span>Raw JSON Spec</span>
          </a>
        </div>
      </div>

      {/* Endpoints Contract Cards */}
      <div className="space-y-6">
        {endpoints.map((ep) => (
          <div key={ep.path} className="glass-panel rounded-2xl p-6 border border-white/10 space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-3">
                <span className="px-2.5 py-1 rounded-lg text-xs font-mono font-bold bg-teal-500/20 text-teal-300 border border-teal-500/30">
                  {ep.method}
                </span>
                <span className="text-base font-mono font-bold text-white">{ep.path}</span>
              </div>
              <span className="text-xs font-semibold text-slate-400">{ep.summary}</span>
            </div>

            <p className="text-xs text-slate-300">{ep.description}</p>

            <div>
              <p className="text-[11px] font-mono text-slate-400 uppercase tracking-wider mb-2 font-bold">
                Example Response Payload (200 OK)
              </p>
              <pre className="p-4 rounded-xl bg-slate-950/80 border border-slate-800 text-teal-300 text-xs font-mono overflow-x-auto">
                {JSON.stringify(ep.responseExample, null, 2)}
              </pre>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
