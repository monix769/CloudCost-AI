import React from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Cloud,
  BarChart3,
  Receipt,
  Target,
  ShieldAlert,
  Lightbulb,
  FileBarChart,
  Settings,
  Layers,
  Activity,
  Cpu,
  BookOpen,
  ChevronLeft,
  ChevronRight,
  X,
  Server,
  Zap,
} from 'lucide-react';

export default function Sidebar({
  collapsed,
  setCollapsed,
  mobileOpen,
  setMobileOpen,
}) {
  const location = useLocation();

  const primaryNav = [
    { to: '/', label: 'Overview', icon: LayoutDashboard },
    { to: '/cloud-accounts', label: 'Cloud Accounts', icon: Cloud, badge: '3' },
    { to: '/analytics', label: 'Cost Analytics', icon: BarChart3 },
    { to: '/billing', label: 'Billing Management', icon: Receipt },
    { to: '/budgets', label: 'Budget Management', icon: Target },
    { to: '/anomalies', label: 'Anomaly Detection', icon: ShieldAlert, badge: '1 Alert', badgeColor: 'bg-rose-500/20 text-rose-300 border-rose-500/30' },
    { to: '/optimization', label: 'Cost Optimization', icon: Lightbulb, badge: '14 Tips', badgeColor: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30' },
    { to: '/reports', label: 'Reports', icon: FileBarChart },
    { to: '/settings', label: 'Settings', icon: Settings },
  ];

  const systemNav = [
    { to: '/architecture', label: 'Architecture', icon: Layers },
    { to: '/health', label: 'Health Probe', icon: Activity },
    { to: '/status', label: 'System Status', icon: Cpu },
    { to: '/docs', label: 'OpenAPI Spec', icon: BookOpen },
  ];

  const handleLinkClick = () => {
    if (mobileOpen) {
      setMobileOpen(false);
    }
  };

  const sidebarContent = (
    <div className="flex flex-col h-full justify-between bg-navy-950 border-r border-white/[0.08] select-none">
      {/* Brand & Logo Header */}
      <div>
        <div className="flex items-center justify-between h-16 px-4 border-b border-white/[0.08]">
          <NavLink to="/" onClick={handleLinkClick} className="flex items-center space-x-3 group">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 via-blue-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-blue-500/25 group-hover:scale-105 transition-transform shrink-0">
              <Zap className="w-5 h-5 text-white font-bold" />
            </div>

            {(!collapsed || mobileOpen) && (
              <div className="transition-opacity duration-200">
                <div className="flex items-center space-x-2">
                  <span className="text-base font-extrabold tracking-tight text-white">CloudCost</span>
                  <span className="text-[10px] px-1.5 py-0.2 rounded font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30 font-mono">
                    AI
                  </span>
                </div>
                <p className="text-[9px] text-slate-400 font-medium tracking-wider uppercase font-mono">
                  FinOps Analytics
                </p>
              </div>
            )}
          </NavLink>

          {/* Mobile close button */}
          {mobileOpen && (
            <button
              onClick={() => setMobileOpen(false)}
              className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 lg:hidden"
            >
              <X className="w-5 h-5" />
            </button>
          )}

          {/* Desktop Collapse Toggle */}
          <button
            onClick={() => setCollapsed(!collapsed)}
            className="hidden lg:flex p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-navy-800/80 transition-colors"
            title={collapsed ? 'Expand Sidebar' : 'Collapse Sidebar'}
          >
            {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
          </button>
        </div>

        {/* Primary FinOps Navigation */}
        <div className="px-3 py-4 space-y-6 overflow-y-auto max-h-[calc(100vh-140px)] scrollbar-none">
          <div className="space-y-1">
            {(!collapsed || mobileOpen) && (
              <p className="px-3 text-[10px] font-mono uppercase tracking-wider font-bold text-slate-400 mb-2">
                FinOps Command
              </p>
            )}

            {primaryNav.map((item) => {
              const Icon = item.icon;
              const isActive =
                item.to === '/'
                  ? location.pathname === '/'
                  : location.pathname.startsWith(item.to);

              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={handleLinkClick}
                  title={collapsed && !mobileOpen ? item.label : undefined}
                  className={`flex items-center justify-between px-3 py-2.5 rounded-xl text-xs font-semibold transition-all group ${
                    isActive
                      ? 'bg-blue-600/20 text-blue-300 border border-blue-500/40 shadow-sm'
                      : 'text-slate-300 hover:text-white hover:bg-navy-800/60 border border-transparent'
                  }`}
                >
                  <div className="flex items-center space-x-3">
                    <Icon
                      className={`w-4 h-4 shrink-0 transition-colors ${
                        isActive ? 'text-blue-400' : 'text-slate-400 group-hover:text-slate-200'
                      }`}
                    />
                    {(!collapsed || mobileOpen) && <span>{item.label}</span>}
                  </div>

                  {(!collapsed || mobileOpen) && item.badge && (
                    <span
                      className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-full border ${
                        item.badgeColor || 'bg-slate-800 text-slate-300 border-slate-700'
                      }`}
                    >
                      {item.badge}
                    </span>
                  )}
                </NavLink>
              );
            })}
          </div>

          {/* System & Architecture Section (Preserving Milestone 1) */}
          <div className="space-y-1 pt-3 border-t border-white/[0.06]">
            {(!collapsed || mobileOpen) && (
              <p className="px-3 text-[10px] font-mono uppercase tracking-wider font-bold text-slate-400 mb-2">
                System Diagnostics
              </p>
            )}

            {systemNav.map((item) => {
              const Icon = item.icon;
              const isActive = location.pathname.startsWith(item.to);

              return (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={handleLinkClick}
                  title={collapsed && !mobileOpen ? item.label : undefined}
                  className={`flex items-center space-x-3 px-3 py-2 rounded-xl text-xs font-medium transition-all group ${
                    isActive
                      ? 'bg-navy-800 text-teal-300 border border-teal-500/30'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-navy-800/40 border border-transparent'
                  }`}
                >
                  <Icon
                    className={`w-4 h-4 shrink-0 ${
                      isActive ? 'text-teal-400' : 'text-slate-400 group-hover:text-slate-300'
                    }`}
                  />
                  {(!collapsed || mobileOpen) && <span>{item.label}</span>}
                </NavLink>
              );
            })}
          </div>
        </div>
      </div>

      {/* Sidebar Footer: Organization and Version */}
      <div className="p-3 border-t border-white/[0.08] bg-navy-950/90">
        {(!collapsed || mobileOpen) ? (
          <div className="flex items-center space-x-3 p-2 rounded-xl bg-navy-900/60 border border-slate-800">
            <div className="w-8 h-8 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center font-bold text-blue-400 text-xs">
              AC
            </div>
            <div className="overflow-hidden">
              <p className="text-xs font-semibold text-white truncate">Acme Cloud Corp</p>
              <p className="text-[10px] text-slate-400 font-mono">Enterprise Tier</p>
            </div>
          </div>
        ) : (
          <div className="flex justify-center">
            <div className="w-8 h-8 rounded-lg bg-blue-600/20 border border-blue-500/30 flex items-center justify-center font-bold text-blue-400 text-xs">
              AC
            </div>
          </div>
        )}
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop Sidebar */}
      <aside
        className={`hidden lg:block fixed inset-y-0 left-0 z-30 transition-all duration-300 ease-in-out ${
          collapsed ? 'w-20' : 'w-64'
        }`}
      >
        {sidebarContent}
      </aside>

      {/* Mobile Backdrop & Drawer */}
      {mobileOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 backdrop-blur-sm lg:hidden transition-opacity"
          onClick={() => setMobileOpen(false)}
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-50 w-72 transform transition-transform duration-300 ease-in-out lg:hidden ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {sidebarContent}
      </aside>
    </>
  );
}
