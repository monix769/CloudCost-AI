import React, { useState } from 'react';
import DemoDataDisclaimer from '../components/DemoDataDisclaimer';
import { Settings, User, Bell, Key, Globe, Shield, Check, Save } from 'lucide-react';

export default function SettingsPage() {
  const [currency, setCurrency] = useState('USD');
  const [slackWebhook, setSlackWebhook] = useState('https://hooks.slack.com/services/T00/B00/XXXXX');
  const [emailAlerts, setEmailAlerts] = useState(true);
  const [saved, setSaved] = useState(false);

  const handleSave = (e) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6 animate-fadeIn max-w-4xl">
      <DemoDataDisclaimer />

      {/* Header */}
      <div className="pb-4 border-b border-slate-800">
        <h2 className="text-xl font-bold text-white flex items-center space-x-2">
          <span>Platform Settings &amp; Governance</span>
        </h2>
        <p className="text-xs text-slate-400 mt-1">
          Manage organization tenant preferences, currency standards, and alerting endpoints
        </p>
      </div>

      <form onSubmit={handleSave} className="space-y-6 text-xs">
        {/* Organization Profile */}
        <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
          <div className="flex items-center space-x-2 pb-3 border-b border-slate-800">
            <User className="w-4 h-4 text-blue-400" />
            <h3 className="font-bold text-white text-sm">Tenant Organization Details</h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-400 mb-1 font-semibold">Organization Legal Name</label>
              <input
                type="text"
                defaultValue="Acme Cloud Corp"
                className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-semibold focus:outline-none focus:border-blue-500"
              />
            </div>

            <div>
              <label className="block text-slate-400 mb-1 font-semibold">Tenant Slug (Identifier)</label>
              <input
                type="text"
                defaultValue="acme-corp"
                disabled
                className="w-full p-2.5 rounded-xl bg-navy-950 border border-slate-800 text-slate-400 font-mono"
              />
            </div>
          </div>
        </div>

        {/* Currency & Financial Standards */}
        <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
          <div className="flex items-center space-x-2 pb-3 border-b border-slate-800">
            <Globe className="w-4 h-4 text-blue-400" />
            <h3 className="font-bold text-white text-sm">Financial Presentation Standards</h3>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-slate-400 mb-1 font-semibold">Base Reporting Currency</label>
              <select
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
                className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
              >
                <option value="USD">USD ($) — United States Dollar</option>
                <option value="EUR">EUR (€) — Eurozone Euro</option>
                <option value="GBP">GBP (£) — British Pound Sterling</option>
                <option value="INR">INR (₹) — Indian Rupee</option>
              </select>
            </div>

            <div>
              <label className="block text-slate-400 mb-1 font-semibold">Fiscal Year Calendar</label>
              <select
                defaultValue="CALENDAR"
                className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
              >
                <option value="CALENDAR">Standard Calendar Year (Jan - Dec)</option>
                <option value="APRIL">April to March Fiscal</option>
                <option value="OCTOBER">October to September Fiscal</option>
              </select>
            </div>
          </div>
        </div>

        {/* Notification Webhooks */}
        <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
          <div className="flex items-center space-x-2 pb-3 border-b border-slate-800">
            <Bell className="w-4 h-4 text-blue-400" />
            <h3 className="font-bold text-white text-sm">Alert Webhooks &amp; Notification Destinations</h3>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-slate-400 mb-1 font-semibold">Slack Alert Incoming Webhook URL</label>
              <input
                type="text"
                value={slackWebhook}
                onChange={(e) => setSlackWebhook(e.target.value)}
                className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono text-xs focus:outline-none focus:border-blue-500"
              />
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-navy-900/60 border border-slate-800">
              <div>
                <p className="font-semibold text-white">Daily FinOps Digest Email</p>
                <p className="text-[11px] text-slate-400">Receive morning spend summaries and alert overviews</p>
              </div>
              <input
                type="checkbox"
                checked={emailAlerts}
                onChange={(e) => setEmailAlerts(e.target.checked)}
                className="w-4 h-4 accent-blue-600 cursor-pointer"
              />
            </div>
          </div>
        </div>

        {/* Save button */}
        <div className="flex items-center justify-between pt-2">
          {saved && (
            <span className="text-xs font-mono text-emerald-400 flex items-center space-x-1.5">
              <Check className="w-4 h-4" />
              <span>Settings successfully persisted (Demonstration Mode).</span>
            </span>
          )}
          <div className="ml-auto">
            <button
              type="submit"
              className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold text-xs shadow-lg shadow-blue-500/20 flex items-center space-x-2 transition-all"
            >
              <Save className="w-4 h-4" />
              <span>Save Configuration</span>
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
