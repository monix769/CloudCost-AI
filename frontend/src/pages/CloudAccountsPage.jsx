import React, { useState } from 'react';
import { cloudAccountsList } from '../utils/mockFinOpsData';
import { Cloud, Plus, CheckCircle, RefreshCw, Shield, Key, ExternalLink, AlertCircle, X } from 'lucide-react';

export default function CloudAccountsPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [newAccount, setNewAccount] = useState({
    provider: 'AWS',
    accountName: '',
    accountId: '',
    roleArn: '',
  });
  const [accounts, setAccounts] = useState(cloudAccountsList);
  const [syncingId, setSyncingId] = useState(null);

  const handleSync = (id) => {
    setSyncingId(id);
    setTimeout(() => {
      setSyncingId(null);
    }, 1200);
  };

  const handleAddAccount = (e) => {
    e.preventDefault();
    if (!newAccount.accountName || !newAccount.accountId) return;

    const created = {
      id: `acc-${Date.now()}`,
      name: newAccount.accountName,
      provider: newAccount.provider,
      accountId: newAccount.accountId,
      region: 'us-east-1 (Auto)',
      currentSpend: 0.0,
      monthlyBudget: 10000.0,
      status: 'ACTIVE',
      syncStatus: 'HEALTHY',
      lastSync: 'Just now',
      activeResources: 0,
    };

    setAccounts([...accounts, created]);
    setModalOpen(false);
    setNewAccount({ provider: 'AWS', accountName: '', accountId: '', roleArn: '' });
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>Multi-Cloud Tenant Connections</span>
            <span className="text-xs px-2 py-0.5 rounded-full font-mono bg-blue-500/10 text-blue-300 border border-blue-500/20">
              {accounts.length} Active Accounts
            </span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Connect IAM roles, Google Service Accounts, and Azure App Registrations to ingest billing records
          </p>
        </div>

        <button
          onClick={() => setModalOpen(true)}
          className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Connect Cloud Account</span>
        </button>
      </div>

      {/* Account Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {accounts.map((acc) => (
          <div key={acc.id} className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 border border-blue-500/20">
                  {acc.provider}
                </span>
                <h3 className="font-bold text-white text-base mt-2">{acc.name}</h3>
                <p className="text-xs font-mono text-slate-400">{acc.accountId}</p>
              </div>

              <button
                onClick={() => handleSync(acc.id)}
                disabled={syncingId === acc.id}
                title="Force Billing Sync"
                className="p-2 rounded-xl bg-navy-900 border border-slate-800 text-slate-400 hover:text-blue-300 hover:border-blue-500/30 transition-colors"
              >
                <RefreshCw className={`w-4 h-4 ${syncingId === acc.id ? 'animate-spin text-blue-400' : ''}`} />
              </button>
            </div>

            <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800 space-y-2 text-xs font-mono">
              <div className="flex justify-between">
                <span className="text-slate-400">Current Spend:</span>
                <span className="font-bold text-white">${acc.currentSpend.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Monthly Budget:</span>
                <span className="text-slate-300">${acc.monthlyBudget.toLocaleString()}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Active Resources:</span>
                <span className="text-emerald-400 font-semibold">{acc.activeResources}</span>
              </div>
            </div>

            <div className="pt-2 flex items-center justify-between text-[11px] text-slate-400 font-mono">
              <span className="flex items-center space-x-1.5 text-emerald-400">
                <CheckCircle className="w-3.5 h-3.5" />
                <span>Sync {acc.syncStatus}</span>
              </span>
              <span>{acc.lastSync}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Connection Guide Note */}
      <div className="rounded-2xl border border-white/[0.08] bg-navy-800/40 p-5 space-y-2 text-xs">
        <h4 className="font-bold text-white flex items-center space-x-2">
          <Shield className="w-4 h-4 text-blue-400" />
          <span>Cross-Account Read-Only Security Architecture</span>
        </h4>
        <p className="text-slate-400 leading-relaxed">
          CloudCost AI operates under strict least-privilege principles. Only read-only permissions for Cost Explorer, Cloud Billing API, and Cost Management export buckets are requested.
        </p>
      </div>

      {/* Modal Simulation */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.1] shadow-2xl max-w-md w-full space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="font-bold text-white text-base">Connect New Cloud Account</h3>
              <button onClick={() => setModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddAccount} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Cloud Provider</label>
                <select
                  value={newAccount.provider}
                  onChange={(e) => setNewAccount({ ...newAccount, provider: e.target.value })}
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                >
                  <option value="AWS">Amazon Web Services (AWS)</option>
                  <option value="GCP">Google Cloud Platform (GCP)</option>
                  <option value="AZURE">Microsoft Azure</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Account / Project Friendly Name</label>
                <input
                  type="text"
                  placeholder="e.g. Staging AWS Workloads"
                  value={newAccount.accountName}
                  onChange={(e) => setNewAccount({ ...newAccount, accountName: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Account / Subscription ID</label>
                <input
                  type="text"
                  placeholder="e.g. 9876-5432-1098"
                  value={newAccount.accountId}
                  onChange={(e) => setNewAccount({ ...newAccount, accountId: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">IAM Role ARN / Credentials Token</label>
                <input
                  type="text"
                  placeholder="arn:aws:iam::123456789012:role/CloudCostReadOnly"
                  value={newAccount.roleArn}
                  onChange={(e) => setNewAccount({ ...newAccount, roleArn: e.target.value })}
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                />
              </div>

              <div className="pt-2 flex items-center justify-end space-x-2">
                <button
                  type="button"
                  onClick={() => setModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:bg-slate-800 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-md"
                >
                  Save Connection
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
