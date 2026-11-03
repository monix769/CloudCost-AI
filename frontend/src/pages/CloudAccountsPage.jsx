import React, { useState, useEffect } from 'react';
import cloudAccountService from '../services/cloudAccountService';
import { Cloud, Plus, CheckCircle, RefreshCw, Shield, Trash2, AlertCircle, X, Loader2 } from 'lucide-react';

export default function CloudAccountsPage() {
  const [modalOpen, setModalOpen] = useState(false);
  const [newAccount, setNewAccount] = useState({
    provider: 'AWS',
    accountName: '',
    accountId: '',
    arnOrClientId: '',
  });
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [syncingId, setSyncingId] = useState(null);

  const fetchAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await cloudAccountService.getAccounts();
      setAccounts(data || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch cloud accounts from server');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAccounts();
  }, []);

  const handleSync = (id) => {
    setSyncingId(id);
    setTimeout(() => {
      setSyncingId(null);
    }, 1000);
  };

  const handleAddAccount = async (e) => {
    e.preventDefault();
    if (!newAccount.accountName || !newAccount.accountId) return;

    try {
      setSubmitting(true);
      setError(null);
      await cloudAccountService.createAccount({
        provider: newAccount.provider,
        accountName: newAccount.accountName,
        accountId: newAccount.accountId,
        arnOrClientId: newAccount.arnOrClientId || null,
        isActive: true,
      });
      setModalOpen(false);
      setNewAccount({ provider: 'AWS', accountName: '', accountId: '', arnOrClientId: '' });
      await fetchAccounts();
    } catch (err) {
      setError(err.message || 'Failed to save cloud account');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteAccount = async (id) => {
    if (!window.confirm('Are you sure you want to delete this cloud account? Associated billing records may be removed.')) {
      return;
    }
    try {
      await cloudAccountService.deleteAccount(id);
      await fetchAccounts();
    } catch (err) {
      alert('Error deleting account: ' + err.message);
    }
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
            Persisted MySQL cloud provider connections for AWS, Azure, and GCP
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

      {/* Error alert if any */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={fetchAccounts} className="underline hover:text-white font-mono">Retry</button>
        </div>
      )}

      {/* Loading State */}
      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {[1, 2, 3].map((n) => (
            <div key={n} className="glass-panel rounded-2xl p-6 border border-white/[0.08] animate-pulse space-y-4">
              <div className="h-6 w-24 bg-slate-800 rounded"></div>
              <div className="h-8 w-48 bg-slate-800 rounded"></div>
              <div className="h-20 bg-slate-800/60 rounded"></div>
            </div>
          ))}
        </div>
      ) : accounts.length === 0 ? (
        /* Empty State */
        <div className="glass-panel rounded-2xl p-12 border border-white/[0.08] text-center space-y-3">
          <Cloud className="w-12 h-12 text-slate-600 mx-auto" />
          <h3 className="text-base font-semibold text-white">No Cloud Accounts Connected</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Connect an AWS account, Azure subscription, or GCP project to begin ingesting billing records and monitoring cloud spend.
          </p>
          <button
            onClick={() => setModalOpen(true)}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white mt-2"
          >
            Connect First Account
          </button>
        </div>
      ) : (
        /* Account Cards */
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {accounts.map((acc) => (
            <div key={acc.id} className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
              <div className="flex items-start justify-between">
                <div>
                  <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 border border-blue-500/20">
                    {acc.provider}
                  </span>
                  <h3 className="font-bold text-white text-base mt-2">{acc.accountName}</h3>
                  <p className="text-xs font-mono text-slate-400">{acc.accountId}</p>
                </div>

                <div className="flex items-center space-x-1">
                  <button
                    onClick={() => handleSync(acc.id)}
                    disabled={syncingId === acc.id}
                    title="Force Billing Sync"
                    className="p-2 rounded-xl bg-navy-900 border border-slate-800 text-slate-400 hover:text-blue-300 hover:border-blue-500/30 transition-colors"
                  >
                    <RefreshCw className={`w-4 h-4 ${syncingId === acc.id ? 'animate-spin text-blue-400' : ''}`} />
                  </button>
                  <button
                    onClick={() => handleDeleteAccount(acc.id)}
                    title="Delete Account"
                    className="p-2 rounded-xl bg-navy-900 border border-slate-800 text-slate-400 hover:text-rose-400 hover:border-rose-500/30 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800 space-y-2 text-xs font-mono">
                <div className="flex justify-between">
                  <span className="text-slate-400">Status:</span>
                  <span className={acc.isActive ? 'text-emerald-400 font-semibold' : 'text-slate-500'}>
                    {acc.isActive ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Sync Status:</span>
                  <span className="text-blue-400 font-semibold">{acc.syncStatus || 'PENDING'}</span>
                </div>
                {acc.arnOrClientId && (
                  <div className="truncate text-slate-500 text-[10px]" title={acc.arnOrClientId}>
                    Ref: {acc.arnOrClientId}
                  </div>
                )}
              </div>

              <div className="pt-2 flex items-center justify-between text-[11px] text-slate-400 font-mono">
                <span className="flex items-center space-x-1.5 text-emerald-400">
                  <CheckCircle className="w-3.5 h-3.5" />
                  <span>Synced</span>
                </span>
                <span>{acc.createdAt ? new Date(acc.createdAt).toLocaleDateString() : 'Active'}</span>
              </div>
            </div>
          ))}
        </div>
      )}

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

      {/* Modal Connection */}
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
                  placeholder="e.g. 123456789012"
                  value={newAccount.accountId}
                  onChange={(e) => setNewAccount({ ...newAccount, accountId: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">IAM Role ARN / Credentials Token (Optional)</label>
                <input
                  type="text"
                  placeholder="arn:aws:iam::123456789012:role/CloudCostReadOnly"
                  value={newAccount.arnOrClientId}
                  onChange={(e) => setNewAccount({ ...newAccount, arnOrClientId: e.target.value })}
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
                  disabled={submitting}
                  className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-md flex items-center space-x-1"
                >
                  {submitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Save Connection</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
