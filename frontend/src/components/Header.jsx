import React, { useState, useRef, useEffect } from 'react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { useSystem } from '../context/SystemContext';
import { useAuth } from '../context/AuthContext';
import ConnectionBadge from './ConnectionBadge';
import {
  Menu,
  Bell,
  Search,
  Calendar,
  ChevronDown,
  User,
  Shield,
  LogOut,
  ExternalLink,
  CheckCircle2,
  AlertTriangle,
  Sparkles,
} from 'lucide-react';

export default function Header({ setMobileOpen, collapsed, setCollapsed }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { isConnected, isDatabaseConnected } = useSystem();
  const { user, logout } = useAuth();

  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [selectedPeriod, setSelectedPeriod] = useState('Month-to-Date (Nov 2026)');

  const notifRef = useRef(null);
  const profileRef = useRef(null);

  const handleLogout = async () => {
    setProfileOpen(false);
    await logout();
    navigate('/login');
  };

  const getInitials = (fullName, email) => {
    if (fullName) {
      const parts = fullName.trim().split(' ');
      if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
      return fullName.substring(0, 2).toUpperCase();
    }
    if (email) return email.substring(0, 2).toUpperCase();
    return 'CC';
  };

  // Close dropdowns on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setNotificationsOpen(false);
      }
      if (profileRef.current && !profileRef.current.contains(event.target)) {
        setProfileOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Compute Page Titles & Subtitles based on Route
  const getRouteDetails = (pathname) => {
    switch (pathname) {
      case '/':
        return {
          title: 'FinOps Command Center',
          subtitle: 'Executive overview of multi-cloud expenditures and spending velocity',
        };
      case '/cloud-accounts':
        return {
          title: 'Cloud Accounts & Tenants',
          subtitle: 'Manage cross-account IAM connections and ingest sync for AWS, GCP, and Azure',
        };
      case '/analytics':
        return {
          title: 'Cost Analytics & Exploration',
          subtitle: 'Deep-dive multi-dimensional spending trends, tag queries, and time-series forecasting',
        };
      case '/billing':
        return {
          title: 'Billing & Invoice Records',
          subtitle: 'Granular invoice line-items, usage unit metrics, and raw expenditure exports',
        };
      case '/budgets':
        return {
          title: 'Budget Management & Guardrails',
          subtitle: 'Establish cost center allocations, thresholds, and proactive financial alerting',
        };
      case '/anomalies':
        return {
          title: 'AI Anomaly Detection',
          subtitle: 'Machine learning identification of abnormal usage spikes and cost outliers',
        };
      case '/optimization':
        return {
          title: 'Cost Optimization Engine',
          subtitle: 'Actionable rightsizing, idle resource reclamation, and commitment discount insights',
        };
      case '/reports':
        return {
          title: 'Executive FinOps Reports',
          subtitle: 'Scheduled executive summaries, unit economics, and showback/chargeback statements',
        };
      case '/settings':
        return {
          title: 'Platform Settings & Governance',
          subtitle: 'Configure currency, notification channels, webhooks, and team access policies',
        };
      case '/architecture':
        return {
          title: 'Enterprise Architecture & Explorer',
          subtitle: 'Interactive 4-tier full-stack system architecture and live REST API explorer',
        };
      case '/health':
        return {
          title: 'Application Health Diagnostics',
          subtitle: 'Real-time Spring Boot, MySQL connectivity, and system storage probes',
        };
      case '/status':
        return {
          title: 'System Runtime Status',
          subtitle: 'JVM memory utilization, active Spring profiles, and enabled platform subsystems',
        };
      case '/docs':
        return {
          title: 'OpenAPI 3 / Swagger Specifications',
          subtitle: 'Interactive REST contracts generated automatically by Springdoc',
        };
      default:
        return {
          title: 'CloudCost AI Platform',
          subtitle: 'Intelligent Cloud Cost Monitoring & FinOps Analytics',
        };
    }
  };

  const { title, subtitle } = getRouteDetails(location.pathname);

  const sampleNotifications = [
    {
      id: 1,
      title: 'AWS EC2 Spend Spike Detected',
      desc: '+38% spending velocity in us-east-1 over 48h baseline',
      time: '15m ago',
      unread: true,
      type: 'warning',
    },
    {
      id: 2,
      title: 'Budget Alert Threshold Reached',
      desc: 'Engineering budget has reached 90.0% of $26,000 allocation',
      time: '2h ago',
      unread: true,
      type: 'alert',
    },
    {
      id: 3,
      title: 'Weekly Cost Optimization Report Ready',
      desc: '$6,420 / mo potential savings identified across 14 recommendations',
      time: '1d ago',
      unread: false,
      type: 'info',
    },
  ];

  return (
    <header className="sticky top-0 z-20 glass-panel border-b border-white/[0.08] bg-navy-950/85">
      <div className="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
        {/* Left: Mobile hamburger & Dynamic Page Titles */}
        <div className="flex items-center space-x-3 sm:space-x-4">
          <button
            onClick={() => setMobileOpen(true)}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-navy-800 lg:hidden"
            title="Open Menu"
          >
            <Menu className="w-5 h-5" />
          </button>

          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-base sm:text-lg font-extrabold text-white tracking-tight">
                {title}
              </h1>
              <span className="hidden sm:inline-block text-[10px] font-mono px-2 py-0.5 rounded font-bold bg-blue-500/10 text-blue-300 border border-blue-500/20">
                FinOps SaaS
              </span>
            </div>
            <p className="hidden md:block text-xs text-slate-400 truncate max-w-xl">
              {subtitle}
            </p>
          </div>
        </div>

        {/* Right Actions: Date Picker, Backend Badge, Notifications, User Menu */}
        <div className="flex items-center space-x-2 sm:space-x-3">
          {/* Quick Date Range Selector */}
          <div className="hidden xl:flex items-center space-x-1.5 px-3 py-1.5 rounded-xl bg-navy-900 border border-slate-800 text-xs text-slate-300 font-mono">
            <Calendar className="w-3.5 h-3.5 text-blue-400" />
            <select
              value={selectedPeriod}
              onChange={(e) => setSelectedPeriod(e.target.value)}
              className="bg-transparent border-none text-slate-200 text-xs focus:outline-none cursor-pointer pr-1"
            >
              <option value="Month-to-Date (Nov 2026)" className="bg-navy-900">Month-to-Date (Nov 2026)</option>
              <option value="Last 30 Days" className="bg-navy-900">Last 30 Days</option>
              <option value="Q3 2026" className="bg-navy-900">Q3 2026</option>
              <option value="Full Year 2026" className="bg-navy-900">Full Year 2026</option>
            </select>
          </div>

          {/* Real-time Backend Connection Indicator */}
          <div className="hidden sm:block">
            <ConnectionBadge />
          </div>

          {/* Notifications Dropdown */}
          <div className="relative" ref={notifRef}>
            <button
              onClick={() => setNotificationsOpen(!notificationsOpen)}
              className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-navy-800/80 border border-transparent hover:border-slate-800 transition-colors relative"
              title="Notifications"
            >
              <Bell className="w-4 h-4" />
              <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-blue-500 ring-2 ring-navy-950"></span>
            </button>

            {notificationsOpen && (
              <div className="absolute right-0 mt-2 w-80 sm:w-96 rounded-2xl glass-panel border border-white/[0.1] shadow-2xl p-4 z-50 animate-fadeIn space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-slate-800">
                  <span className="text-xs font-bold text-white uppercase font-mono tracking-wider">
                    FinOps Notifications (2 New)
                  </span>
                  <button
                    onClick={() => setNotificationsOpen(false)}
                    className="text-[11px] text-blue-400 hover:underline"
                  >
                    Mark all read
                  </button>
                </div>

                <div className="space-y-2 max-h-72 overflow-y-auto">
                  {sampleNotifications.map((n) => (
                    <div
                      key={n.id}
                      className={`p-3 rounded-xl border text-xs space-y-1 transition-colors ${
                        n.unread
                          ? 'bg-blue-950/30 border-blue-500/30 text-slate-200'
                          : 'bg-navy-900/40 border-slate-800 text-slate-400'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-white">{n.title}</span>
                        <span className="text-[10px] font-mono text-slate-400">{n.time}</span>
                      </div>
                      <p className="text-[11px] text-slate-300">{n.desc}</p>
                    </div>
                  ))}
                </div>

                <div className="pt-2 border-t border-slate-800 text-center">
                  <Link
                    to="/anomalies"
                    onClick={() => setNotificationsOpen(false)}
                    className="text-xs font-semibold text-blue-400 hover:text-blue-300"
                  >
                    View Anomaly Center →
                  </Link>
                </div>
              </div>
            )}
          </div>

          {/* User Profile Menu */}
          <div className="relative" ref={profileRef}>
            <button
              onClick={() => setProfileOpen(!profileOpen)}
              className="flex items-center space-x-2.5 p-1.5 rounded-xl hover:bg-navy-800/80 border border-transparent hover:border-slate-800 transition-colors cursor-pointer"
            >
              <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-xs font-bold text-white shadow-sm">
                {getInitials(user?.fullName, user?.email)}
              </div>
              <div className="hidden md:block text-left">
                <p className="text-xs font-bold text-white leading-tight">
                  {user?.fullName || 'Authenticated User'}
                </p>
                <p className="text-[10px] text-slate-400 font-mono">
                  {user?.role ? user.role.replace('ROLE_', '') : 'USER'}
                </p>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400 hidden sm:block" />
            </button>

            {profileOpen && (
              <div className="absolute right-0 mt-2 w-64 rounded-2xl glass-panel border border-white/[0.1] shadow-2xl p-4 z-50 animate-fadeIn space-y-3">
                <div className="pb-3 border-b border-slate-800">
                  <p className="text-xs font-bold text-white">{user?.fullName || 'FinOps Member'}</p>
                  <p className="text-[11px] text-slate-400 font-mono truncate">{user?.email}</p>
                  <div className="mt-2 flex items-center space-x-1.5">
                    <span className="text-[10px] px-2 py-0.5 rounded font-mono font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30 truncate max-w-[130px]">
                      {user?.organizationName || 'Acme Cloud Corp'}
                    </span>
                    <span className="text-[10px] px-2 py-0.5 rounded font-mono font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                      {user?.role ? user.role.replace('ROLE_', '') : 'USER'}
                    </span>
                  </div>
                </div>

                <div className="space-y-1 text-xs">
                  <Link
                    to="/settings"
                    onClick={() => setProfileOpen(false)}
                    className="flex items-center space-x-2 px-2.5 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-navy-800 transition-colors"
                  >
                    <User className="w-4 h-4 text-slate-400" />
                    <span>Profile &amp; Governance</span>
                  </Link>

                  <Link
                    to="/cloud-accounts"
                    onClick={() => setProfileOpen(false)}
                    className="flex items-center space-x-2 px-2.5 py-2 rounded-lg text-slate-300 hover:text-white hover:bg-navy-800 transition-colors"
                  >
                    <Shield className="w-4 h-4 text-slate-400" />
                    <span>Cloud IAM Credentials</span>
                  </Link>

                  {user?.role === 'ROLE_ADMIN' && (
                    <Link
                      to="/cloud-accounts"
                      onClick={() => setProfileOpen(false)}
                      className="flex items-center space-x-2 px-2.5 py-2 rounded-lg text-blue-300 hover:text-white hover:bg-blue-600/20 transition-colors font-medium"
                    >
                      <Sparkles className="w-4 h-4 text-blue-400" />
                      <span>Admin Tenant Controls</span>
                    </Link>
                  )}

                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center space-x-2 px-2.5 py-2 rounded-lg text-rose-300 hover:text-white hover:bg-rose-500/20 transition-colors cursor-pointer text-left"
                  >
                    <LogOut className="w-4 h-4 text-rose-400" />
                    <span>Sign Out</span>
                  </button>
                </div>

                <div className="pt-2 border-t border-slate-800 text-[11px] text-slate-400">
                  <p>Milestone 4 • JWT Auth Active</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
