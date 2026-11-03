import React from 'react';
import { Layers, Shield, Server, Database, ArrowRight, ArrowDown } from 'lucide-react';

export default function ArchitectureDiagram() {
  const tiers = [
    {
      name: 'Client Tier',
      tech: 'React 19 • Vite • Tailwind CSS',
      icon: Layers,
      color: 'border-cyan-500/30 bg-cyan-950/20 text-cyan-300',
      description: 'Single Page Application (SPA), centralized Axios API service, responsive FinOps shell.',
      features: ['Axios Interceptor', 'Tailwind Design System', 'React Router DOM']
    },
    {
      name: 'Security & Gateway Tier',
      tech: 'Spring Security 6 • CORS • Stateless',
      icon: Shield,
      color: 'border-teal-500/30 bg-teal-950/20 text-teal-300',
      description: 'CORS policy configured for React origin, stateless session management, BCrypt hashing.',
      features: ['Stateless FilterChain', 'Configured CORS Filter', 'JWT Architecture Prepared']
    },
    {
      name: 'Application Core Tier',
      tech: 'Spring Boot 3.3.5 • Java 21 • OpenAPI',
      icon: Server,
      color: 'border-blue-500/30 bg-blue-950/20 text-blue-300',
      description: 'Controller-Service-Repository pattern, standardized ApiResponse, centralized RestControllerAdvice.',
      features: ['Root, Health, Status Endpoints', 'Global Exception Handler', 'Swagger UI v3 Docs']
    },
    {
      name: 'Persistence & Data Tier',
      tech: 'Spring Data JPA • Hibernate • MySQL 8',
      icon: Database,
      color: 'border-emerald-500/30 bg-emerald-950/20 text-emerald-300',
      description: 'HikariCP connection pool, schema validation, multi-tenant billing & budget data model.',
      features: ['HikariCP Connection Pool', 'BaseEntity Auditing', 'Safe DDL Auto (update/validate)']
    }
  ];

  return (
    <div className="glass-panel rounded-2xl p-6 border border-white/10 shadow-xl">
      <div className="mb-6">
        <h2 className="text-lg font-bold text-white flex items-center space-x-2">
          <span>Enterprise Full-Stack Architecture</span>
          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-teal-500/10 text-teal-300 border border-teal-500/20">
            SOLID &amp; Clean Architecture
          </span>
        </h2>
        <p className="text-xs text-slate-400 mt-1">
          Strict separation of concerns across presentation, security, business logic, and relational persistence.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 relative">
        {tiers.map((tier, index) => {
          const Icon = tier.icon;
          return (
            <div
              key={tier.name}
              className={`p-4 rounded-xl border ${tier.color} flex flex-col justify-between transition-all hover:scale-[1.02]`}
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className="p-2 rounded-lg bg-black/30 border border-white/5">
                    <Icon className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] font-mono uppercase tracking-wider font-bold text-slate-400">
                    Tier 0{index + 1}
                  </span>
                </div>

                <h3 className="font-bold text-sm text-white mb-0.5">{tier.name}</h3>
                <p className="text-[11px] font-mono text-teal-400/90 mb-2">{tier.tech}</p>
                <p className="text-xs text-slate-300 leading-relaxed mb-4">{tier.description}</p>
              </div>

              <div className="pt-3 border-t border-white/5">
                <p className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mb-1.5">Capabilities</p>
                <div className="space-y-1">
                  {tier.features.map((feat) => (
                    <div key={feat} className="text-[11px] text-slate-300 flex items-center space-x-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-teal-400"></span>
                      <span>{feat}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
