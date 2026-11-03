import React from 'react';
import { NavLink } from 'react-router-dom';
import ConnectionBadge from './ConnectionBadge';
import { Cloud, Activity, Cpu, BookOpen, Layers } from 'lucide-react';

export default function Navbar() {
  const navItems = [
    { to: '/', label: 'Architecture', icon: Layers },
    { to: '/health', label: 'Health Probe', icon: Activity },
    { to: '/status', label: 'System Status', icon: Cpu },
    { to: '/docs', label: 'OpenAPI & Swagger', icon: BookOpen },
  ];

  return (
    <header className="sticky top-0 z-50 glass-panel border-b border-white/5 bg-[#0b0f19]/80">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo & Brand */}
          <NavLink to="/" className="flex items-center space-x-3 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-teal-500 via-teal-400 to-cyan-500 flex items-center justify-center shadow-lg shadow-teal-500/20 group-hover:scale-105 transition-transform">
              <Cloud className="w-5 h-5 text-slate-950 font-bold" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="text-lg font-black tracking-tight text-white">CloudCost</span>
                <span className="text-xs px-2 py-0.5 rounded-full font-bold bg-teal-500/20 text-teal-300 border border-teal-500/30">AI</span>
              </div>
              <p className="text-[10px] text-slate-400 font-medium tracking-wide">FINOPS ANALYTICS PLATFORM</p>
            </div>
          </NavLink>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center space-x-1">
            {navItems.map(({ to, label, icon: Icon }) => (
              <NavLink
                key={to}
                to={to}
                className={({ isActive }) =>
                  `flex items-center space-x-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all ${
                    isActive
                      ? 'bg-teal-500/15 text-teal-300 border border-teal-500/30 shadow-sm'
                      : 'text-slate-300 hover:text-white hover:bg-slate-800/60 border border-transparent'
                  }`
                }
              >
                <Icon className="w-4 h-4" />
                <span>{label}</span>
              </NavLink>
            ))}
          </nav>

          {/* Live Status Badge */}
          <div className="flex items-center space-x-4">
            <ConnectionBadge />
          </div>
        </div>
      </div>
    </header>
  );
}
