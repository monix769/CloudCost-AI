import React from 'react';
import { useSystem } from '../context/SystemContext';
import StatusCard from '../components/StatusCard';
import EndpointTester from '../components/EndpointTester';
import ArchitectureDiagram from '../components/ArchitectureDiagram';
import { formatUptime, formatDate } from '../utils/formatters';
import { 
  Server, 
  Database, 
  ShieldCheck, 
  Clock, 
  Activity, 
  CheckCircle2, 
  ExternalLink,
  ChevronRight,
  Terminal,
  Cpu
} from 'lucide-react';
import { Link } from 'react-router-dom';

export default function HomePage() {
  const { health, rootInfo, isConnected, isDatabaseConnected, lastUpdated } = useSystem();

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Hero Section */}
      <section className="relative overflow-hidden glass-panel rounded-3xl p-8 sm:p-10 border border-white/10 shadow-2xl">
        <div className="absolute -right-20 -top-20 w-96 h-96 bg-teal-500/10 rounded-full blur-3xl pointer-events-none"></div>
        <div className="absolute -left-20 -bottom-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none"></div>

        <div className="relative z-10 max-w-3xl space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-teal-500/10 border border-teal-500/20 text-teal-300 text-xs font-semibold">
            <span className="w-2 h-2 rounded-full bg-teal-400 animate-pulse"></span>
            <span>Milestone 1 — Project Foundation &amp; Architecture Established</span>
          </div>

          <h1 className="text-3xl sm:text-5xl font-extrabold text-white tracking-tight leading-tight">
            Intelligent Cloud Cost Monitoring &amp; <span className="text-transparent bg-clip-text bg-gradient-to-r from-teal-400 to-cyan-400">FinOps Analytics</span>
          </h1>

          <p className="text-sm sm:text-base text-slate-300 leading-relaxed">
            Enterprise full-stack SaaS platform built with <span className="text-teal-300 font-semibold">Java 21</span>, <span className="text-teal-300 font-semibold">Spring Boot 3</span>, and <span className="text-teal-300 font-semibold">React 19</span>. Designed to monitor multi-cloud expenditures across AWS, GCP, and Azure with strict clean architecture and automated health diagnostics.
          </p>

          <div className="pt-2 flex flex-wrap gap-3 text-xs">
            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-xl bg-teal-500/20 hover:bg-teal-500/30 text-teal-300 border border-teal-500/30 font-semibold transition-all hover:scale-105"
            >
              <span>Swagger UI v3</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>

            <Link
              to="/health"
              className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 text-slate-200 border border-slate-700 font-semibold transition-all"
            >
              <span>Health Diagnostics</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </Link>

            <Link
              to="/status"
              className="inline-flex items-center space-x-2 px-4 py-2.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 text-slate-200 border border-slate-700 font-semibold transition-all"
            >
              <span>System Status</span>
              <ChevronRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </section>

      {/* KPI Status Cards Grid */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatusCard
          icon={Server}
          title="Backend API Status"
          value={isConnected ? 'ONLINE' : 'CONNECTING...'}
          subtitle="Spring Boot 3.3.5 • Java 21"
          status={isConnected ? '200 OK — localhost:8080' : 'Awaiting Connection'}
          badgeColor="teal"
        />

        <StatusCard
          icon={Database}
          title="MySQL 8+ Database"
          value={isDatabaseConnected ? 'CONNECTED' : 'STANDBY'}
          subtitle="InnoDB • HikariCP Pool"
          status={isDatabaseConnected ? 'Schema Verified (cloudcost_ai)' : 'Connecting to MySQL'}
          badgeColor="emerald"
        />

        <StatusCard
          icon={ShieldCheck}
          title="Security Filter Chain"
          value="STATELESS"
          subtitle="Spring Security 6 • CORS Enabled"
          status="JWT Architecture Prepared"
          badgeColor="blue"
        />

        <StatusCard
          icon={Clock}
          title="Application Uptime"
          value={formatUptime(health?.uptimeSeconds || 0)}
          subtitle={`Checked: ${lastUpdated ? lastUpdated.toLocaleTimeString() : 'Pending'}`}
          status="Active JVM Process"
          badgeColor="purple"
        />
      </section>

      {/* Architecture Visual Diagram */}
      <section>
        <ArchitectureDiagram />
      </section>

      {/* Interactive REST API Explorer */}
      <section>
        <EndpointTester />
      </section>

      {/* FinOps Roadmap & Upcoming Milestones */}
      <section className="glass-panel rounded-2xl p-6 border border-white/10">
        <h2 className="text-lg font-bold text-white mb-2">FinOps Platform Implementation Roadmap</h2>
        <p className="text-xs text-slate-400 mb-6">
          Architectural blueprint showing completed foundation and planned features for subsequent milestones.
        </p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Milestone 1 */}
          <div className="p-4 rounded-xl border border-teal-500/40 bg-teal-950/20 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-mono font-bold text-teal-400">MILESTONE 1</span>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-bold bg-teal-500/20 text-teal-300 border border-teal-500/30">
                COMPLETED
              </span>
            </div>
            <h3 className="font-bold text-sm text-white">Foundation &amp; Architecture</h3>
            <ul className="text-xs text-slate-300 space-y-1 pt-1">
              <li className="flex items-center space-x-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>Spring Boot 3 + Java 21 REST API</span>
              </li>
              <li className="flex items-center space-x-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>React 19 + Vite + Tailwind Shell</span>
              </li>
              <li className="flex items-center space-x-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>MySQL 8 schema &amp; HikariCP</span>
              </li>
              <li className="flex items-center space-x-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>Root, Health, Status endpoints</span>
              </li>
              <li className="flex items-center space-x-1.5">
                <CheckCircle2 className="w-3.5 h-3.5 text-teal-400" />
                <span>Postman suite &amp; JUnit 5 tests</span>
              </li>
            </ul>
          </div>

          {/* Milestone 2 */}
          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/40 space-y-2 opacity-80">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-mono font-bold text-slate-400">MILESTONE 2</span>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-bold bg-slate-800 text-slate-400 border border-slate-700">
                UPCOMING
              </span>
            </div>
            <h3 className="font-bold text-sm text-white">Auth &amp; Multi-Tenancy</h3>
            <ul className="text-xs text-slate-400 space-y-1 pt-1">
              <li>• Spring Security JWT filter injection</li>
              <li>• User Registration &amp; BCrypt login</li>
              <li>• Multi-tenant organization boundaries</li>
              <li>• Role-Based Access Control (RBAC)</li>
            </ul>
          </div>

          {/* Milestone 3 */}
          <div className="p-4 rounded-xl border border-slate-800 bg-slate-900/40 space-y-2 opacity-80">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-mono font-bold text-slate-400">MILESTONE 3</span>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-bold bg-slate-800 text-slate-400 border border-slate-700">
                UPCOMING
              </span>
            </div>
            <h3 className="font-bold text-sm text-white">Billing &amp; Cloud Analytics</h3>
            <ul className="text-xs text-slate-400 space-y-1 pt-1">
              <li>• AWS, Azure, GCP billing ingest</li>
              <li>• FinOps Chart.js visual dashboards</li>
              <li>• Budget tracking &amp; threshold alerts</li>
              <li>• Anomaly detection &amp; recommendations</li>
            </ul>
          </div>
        </div>
      </section>
    </div>
  );
}
