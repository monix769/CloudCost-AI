import React, { useState, useEffect } from 'react';
import cloudAccountService from '../services/cloudAccountService';
import billingService from '../services/billingService';
import {
  Cloud,
  Plus,
  CheckCircle2,
  RefreshCw,
  Shield,
  ShieldCheck,
  Trash2,
  AlertCircle,
  AlertTriangle,
  X,
  Loader2,
  Key,
  History,
  Unlink,
  ExternalLink,
  Lock,
  Database,
  Calendar,
  Check,
} from 'lucide-react';

export default function CloudAccountsPage() {
  const [accounts, setAccounts] = useState([]);
  const [providers, setProviders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);

  // Modals state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [credModalOpen, setCredModalOpen] = useState(false);
  const [historyModalOpen, setHistoryModalOpen] = useState(false);

  // Selected account for creds/history
  const [selectedAccount, setSelectedAccount] = useState(null);

  // Action loaders
  const [validatingId, setValidatingId] = useState(null);
  const [syncingId, setSyncingId] = useState(null);
  const [submittingCreds, setSubmittingCreds] = useState(false);
  const [testingConnection, setTestingConnection] = useState(false);
  const [testResult, setTestResult] = useState(null);

  // Sync History data
  const [syncHistory, setSyncHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  // New account form
  const [newAccount, setNewAccount] = useState({
    provider: 'AWS',
    accountName: '',
    accountId: '',
    arnOrClientId: '',
  });

  // Credential modal form
  const [credForm, setCredForm] = useState({
    useMock: true,
    // AWS
    accessKeyId: '',
    secretAccessKey: '',
    roleArn: '',
    region: 'us-east-1',
    // Azure
    tenantId: '',
    clientId: '',
    clientSecret: '',
    subscriptionId: '',
    // GCP
    projectId: '',
    datasetId: 'billing_export',
    tableName: 'gcp_billing_export_v1_standard',
    serviceAccountJson: '',
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [accountsData, providersData] = await Promise.all([
        cloudAccountService.getAccounts(),
        cloudAccountService.getProviders().catch(() => []),
      ]);
      setAccounts(accountsData || []);
      setProviders(providersData || []);
    } catch (err) {
      setError(err.message || 'Failed to fetch cloud account information');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Show auto-dismiss toast message
  const notify = (type, message) => {
    setActionMessage({ type, message });
    setTimeout(() => setActionMessage(null), 6000);
  };

  // 1. Create account
  const handleAddAccount = async (e) => {
    e.preventDefault();
    if (!newAccount.accountName || !newAccount.accountId) return;

    try {
      setLoading(true);
      await cloudAccountService.createAccount({
        provider: newAccount.provider,
        accountName: newAccount.accountName,
        accountId: newAccount.accountId,
        arnOrClientId: newAccount.arnOrClientId || null,
        isActive: true,
      });
      setCreateModalOpen(false);
      setNewAccount({ provider: 'AWS', accountName: '', accountId: '', arnOrClientId: '' });
      notify('success', 'Cloud account created successfully');
      await fetchData();
    } catch (err) {
      notify('error', err.response?.data?.message || err.message || 'Failed to save cloud account');
    } finally {
      setLoading(false);
    }
  };

  // 2. Open Credential modal
  const openCredentialModal = (account) => {
    setSelectedAccount(account);
    setTestResult(null);
    setCredForm({
      useMock: true,
      accessKeyId: '',
      secretAccessKey: '',
      roleArn: account.arnOrClientId || '',
      region: 'us-east-1',
      tenantId: '',
      clientId: account.arnOrClientId || '',
      clientSecret: '',
      subscriptionId: account.accountId || '',
      projectId: account.accountId || '',
      datasetId: 'billing_export',
      tableName: 'gcp_billing_export_v1_standard',
      serviceAccountJson: '',
    });
    setCredModalOpen(true);
  };

  // 3. Test Connection
  const handleTestConnection = async () => {
    if (!selectedAccount) return;
    try {
      setTestingConnection(true);
      setTestResult(null);
      const res = await cloudAccountService.testProviderConnection(selectedAccount.provider, {
        provider: selectedAccount.provider,
        useMock: credForm.useMock,
        accessKeyId: credForm.accessKeyId,
        secretAccessKey: credForm.secretAccessKey,
        roleArn: credForm.roleArn,
        region: credForm.region,
        tenantId: credForm.tenantId,
        clientId: credForm.clientId,
        clientSecret: credForm.clientSecret,
        subscriptionId: credForm.subscriptionId,
        projectId: credForm.projectId,
        datasetId: credForm.datasetId,
        tableName: credForm.tableName,
        serviceAccountJson: credForm.serviceAccountJson,
      });
      setTestResult(res);
    } catch (err) {
      setTestResult({
        valid: false,
        message: err.response?.data?.message || err.message || 'Connection test failed',
      });
    } finally {
      setTestingConnection(false);
    }
  };

  // 4. Save & Connect Credentials (AES-256-GCM encrypted)
  const handleSaveCredentials = async (e) => {
    e.preventDefault();
    if (!selectedAccount) return;
    try {
      setSubmittingCreds(true);
      const res = await cloudAccountService.connectAccount(selectedAccount.id, {
        provider: selectedAccount.provider,
        useMock: credForm.useMock,
        accessKeyId: credForm.accessKeyId,
        secretAccessKey: credForm.secretAccessKey,
        roleArn: credForm.roleArn,
        region: credForm.region,
        tenantId: credForm.tenantId,
        clientId: credForm.clientId,
        clientSecret: credForm.clientSecret,
        subscriptionId: credForm.subscriptionId,
        projectId: credForm.projectId,
        datasetId: credForm.datasetId,
        tableName: credForm.tableName,
        serviceAccountJson: credForm.serviceAccountJson,
      });
      setCredModalOpen(false);
      notify('success', `Credentials encrypted with AES-256-GCM (v1) and connected for ${selectedAccount.accountName}`);
      await fetchData();
    } catch (err) {
      notify('error', err.response?.data?.message || err.message || 'Failed to save credentials');
    } finally {
      setSubmittingCreds(false);
    }
  };

  // 5. Validate Account Connection
  const handleValidateAccount = async (account) => {
    try {
      setValidatingId(account.id);
      const res = await cloudAccountService.validateAccount(account.id);
      if (res.valid) {
        notify('success', `Connection Validated: ${res.message} (Status: ${res.connectionStatus})`);
      } else {
        notify('error', `Validation Failed: ${res.message}`);
      }
      await fetchData();
    } catch (err) {
      notify('error', err.response?.data?.message || err.message || 'Validation request failed');
    } finally {
      setValidatingId(null);
    }
  };

  // 6. Trigger Billing Synchronization
  const handleSyncNow = async (account) => {
    try {
      setSyncingId(account.id);
      const res = await billingService.triggerSync(account.id);
      notify(
        'success',
        `Sync Finished: ${res.recordsInserted} inserted, ${res.recordsSkipped} duplicate skipped (${res.recordsFetched} total fetched from ${res.provider})`
      );
      await fetchData();
    } catch (err) {
      notify('error', err.response?.data?.message || err.message || 'Sync execution failed');
    } finally {
      setSyncingId(null);
    }
  };

  // 7. Disconnect Account
  const handleDisconnect = async (account) => {
    if (!window.confirm(`Disconnect credentials for ${account.accountName}? Encrypted credentials will be securely wiped.`)) {
      return;
    }
    try {
      await cloudAccountService.disconnectAccount(account.id);
      notify('success', `Account ${account.accountName} disconnected and credentials purged`);
      await fetchData();
    } catch (err) {
      notify('error', err.response?.data?.message || err.message || 'Failed to disconnect account');
    }
  };

  // 8. Delete Account
  const handleDeleteAccount = async (id) => {
    if (!window.confirm('Are you sure you want to delete this cloud account? Associated billing records may also be removed.')) {
      return;
    }
    try {
      await cloudAccountService.deleteAccount(id);
      notify('success', 'Cloud account deleted');
      await fetchData();
    } catch (err) {
      notify('error', err.response?.data?.message || err.message || 'Error deleting account');
    }
  };

  // 9. View Sync History
  const openHistoryModal = async (account) => {
    setSelectedAccount(account);
    setHistoryModalOpen(true);
    try {
      setHistoryLoading(true);
      const res = await billingService.getSyncHistory({ cloudAccountId: account.id, size: 20 });
      setSyncHistory(res?.content || []);
    } catch (err) {
      setSyncHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  const getProviderTheme = (provider) => {
    switch (provider) {
      case 'AWS':
        return {
          badge: 'bg-amber-500/15 text-amber-300 border-amber-500/30',
          border: 'border-amber-500/20',
        };
      case 'GCP':
        return {
          badge: 'bg-sky-500/15 text-sky-300 border-sky-500/30',
          border: 'border-sky-500/20',
        };
      case 'AZURE':
        return {
          badge: 'bg-indigo-500/15 text-indigo-300 border-indigo-500/30',
          border: 'border-indigo-500/20',
        };
      default:
        return {
          badge: 'bg-slate-800 text-slate-300 border-slate-700',
          border: 'border-slate-800',
        };
    }
  };

  const getConnectionStatusBadge = (status) => {
    switch (status) {
      case 'CONNECTED':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
            <CheckCircle2 className="w-3 h-3" />
            <span>CONNECTED</span>
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-rose-500/15 text-rose-300 border border-rose-500/30">
            <AlertCircle className="w-3 h-3" />
            <span>FAILED</span>
          </span>
        );
      case 'PENDING':
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-amber-500/15 text-amber-300 border border-amber-500/30">
            <RefreshCw className="w-3 h-3 animate-spin" />
            <span>PENDING</span>
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center space-x-1 px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-slate-800 text-slate-400 border border-slate-700">
            <Unlink className="w-3 h-3" />
            <span>DISCONNECTED</span>
          </span>
        );
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <span>Multi-Cloud Provider Ingestion &amp; Accounts</span>
            <span className="text-xs px-2 py-0.5 rounded-full font-mono bg-blue-500/10 text-blue-300 border border-blue-500/20">
              {accounts.length} Configured
            </span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Secure multi-tenant cloud cost ingestion for AWS Cost Explorer, Azure Cost Management, and GCP BigQuery Export.
          </p>
        </div>

        <button
          onClick={() => setCreateModalOpen(true)}
          className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>Add Cloud Account</span>
        </button>
      </div>

      {/* Global Notifications */}
      {actionMessage && (
        <div
          className={`p-4 rounded-xl text-xs flex items-center justify-between border animate-fadeIn ${
            actionMessage.type === 'success'
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
              : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
          }`}
        >
          <div className="flex items-center space-x-2">
            {actionMessage.type === 'success' ? (
              <CheckCircle2 className="w-4 h-4 shrink-0" />
            ) : (
              <AlertCircle className="w-4 h-4 shrink-0" />
            )}
            <span className="font-medium">{actionMessage.message}</span>
          </div>
          <button onClick={() => setActionMessage(null)} className="text-slate-400 hover:text-white">
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* Cloud Provider Capabilities Banner Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {providers.map((p) => {
          const theme = getProviderTheme(p.provider);
          return (
            <div
              key={p.provider}
              className={`glass-panel rounded-2xl p-4 border ${theme.border} space-y-3 relative overflow-hidden`}
            >
              <div className="flex items-center justify-between">
                <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${theme.badge}`}>
                  {p.provider}
                </span>
                <span className="text-[10px] font-mono text-emerald-400 flex items-center space-x-1">
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>SPI v5 Ready</span>
                </span>
              </div>
              <div>
                <h4 className="font-bold text-white text-sm">{p.providerName}</h4>
                <p className="text-[11px] text-slate-300 font-mono mt-0.5 truncate" title={p.supportedApi}>
                  {p.supportedApi}
                </p>
              </div>
              <div className="space-y-1 text-[11px] text-slate-400 font-mono">
                <div className="flex justify-between">
                  <span>Reporting Lag:</span>
                  <span className="text-slate-300">{p.reportingLag}</span>
                </div>
                <div className="flex justify-between">
                  <span>Auth Method:</span>
                  <span className="text-slate-300 truncate max-w-[150px]" title={p.authType}>
                    {p.authType}
                  </span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Account Cards */}
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
        <div className="glass-panel rounded-2xl p-12 border border-white/[0.08] text-center space-y-3">
          <Cloud className="w-12 h-12 text-slate-600 mx-auto" />
          <h3 className="text-base font-semibold text-white">No Cloud Accounts Configured</h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Add an AWS, Azure, or GCP account and connect encrypted API credentials to begin live automated billing synchronization.
          </p>
          <button
            onClick={() => setCreateModalOpen(true)}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white mt-2"
          >
            Add First Account
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
          {accounts.map((acc) => {
            const theme = getProviderTheme(acc.provider);
            const isValidating = validatingId === acc.id;
            const isSyncing = syncingId === acc.id;

            return (
              <div
                key={acc.id}
                className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4 flex flex-col justify-between"
              >
                <div className="space-y-4">
                  {/* Top Bar */}
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded border ${theme.badge}`}>
                          {acc.provider}
                        </span>
                        {getConnectionStatusBadge(acc.connectionStatus)}
                      </div>
                      <h3 className="font-bold text-white text-base mt-2">{acc.accountName}</h3>
                      <p className="text-xs font-mono text-slate-400">{acc.accountId}</p>
                    </div>

                    <div className="flex items-center space-x-1">
                      <button
                        onClick={() => handleDeleteAccount(acc.id)}
                        title="Delete Account"
                        className="p-1.5 rounded-lg bg-navy-900 border border-slate-800 text-slate-400 hover:text-rose-400 hover:border-rose-500/30 transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Metadata Card */}
                  <div className="p-3 rounded-xl bg-navy-900/60 border border-slate-800 space-y-2 text-xs font-mono">
                    <div className="flex justify-between items-center">
                      <span className="text-slate-400">Credentials:</span>
                      {acc.credentialsConfigured ? (
                        <span className="inline-flex items-center space-x-1 text-emerald-400 font-semibold text-[11px]">
                          <Lock className="w-3 h-3" />
                          <span>AES-256 ({acc.keyVersion || 'v1'})</span>
                        </span>
                      ) : (
                        <span className="text-slate-500 text-[11px]">Unconfigured</span>
                      )}
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-slate-400">Sync Status:</span>
                      <span className="text-blue-400 font-semibold">{acc.syncStatus || 'PENDING'}</span>
                    </div>
                    <div className="flex justify-between items-center">
                      <span className="text-slate-400">Last Synced:</span>
                      <span className="text-slate-300 text-[11px]">
                        {acc.lastSyncedAt ? new Date(acc.lastSyncedAt).toLocaleString() : 'Never'}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Actions Grid */}
                <div className="space-y-2 pt-2 border-t border-slate-800/80">
                  <div className="grid grid-cols-2 gap-2">
                    {/* Connect / Edit Credentials */}
                    <button
                      onClick={() => openCredentialModal(acc)}
                      className="px-3 py-1.5 rounded-xl bg-navy-900 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold inline-flex items-center justify-center space-x-1.5 transition-all"
                    >
                      <Key className="w-3.5 h-3.5 text-amber-400" />
                      <span>{acc.credentialsConfigured ? 'Edit Keys' : 'Attach Keys'}</span>
                    </button>

                    {/* Validate Connection */}
                    <button
                      onClick={() => handleValidateAccount(acc)}
                      disabled={!acc.credentialsConfigured || isValidating}
                      className="px-3 py-1.5 rounded-xl bg-navy-900 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold inline-flex items-center justify-center space-x-1.5 transition-all disabled:opacity-40"
                    >
                      {isValidating ? (
                        <Loader2 className="w-3.5 h-3.5 animate-spin text-blue-400" />
                      ) : (
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                      )}
                      <span>Validate</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    {/* Sync Now */}
                    <button
                      onClick={() => handleSyncNow(acc)}
                      disabled={!acc.credentialsConfigured || isSyncing}
                      className="px-3 py-1.5 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-40 text-white text-xs font-semibold inline-flex items-center justify-center space-x-1.5 shadow-sm transition-all"
                    >
                      <RefreshCw className={`w-3.5 h-3.5 ${isSyncing ? 'animate-spin' : ''}`} />
                      <span>{isSyncing ? 'Syncing...' : 'Sync Now'}</span>
                    </button>

                    {/* Sync History Audit */}
                    <button
                      onClick={() => openHistoryModal(acc)}
                      className="px-3 py-1.5 rounded-xl bg-navy-900 hover:bg-slate-800 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold inline-flex items-center justify-center space-x-1.5 transition-all"
                    >
                      <History className="w-3.5 h-3.5 text-sky-400" />
                      <span>Audit Logs</span>
                    </button>
                  </div>

                  {acc.credentialsConfigured && (
                    <button
                      onClick={() => handleDisconnect(acc)}
                      className="w-full text-center text-[10px] text-rose-400/80 hover:text-rose-300 font-mono py-1 hover:underline flex items-center justify-center space-x-1"
                    >
                      <Unlink className="w-3 h-3" />
                      <span>Disconnect &amp; Wipe Encrypted Credentials</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Security Architecture Footnote */}
      <div className="rounded-2xl border border-white/[0.08] bg-navy-800/40 p-5 space-y-2 text-xs">
        <h4 className="font-bold text-white flex items-center space-x-2">
          <Shield className="w-4 h-4 text-blue-400" />
          <span>Zero-Knowledge Credential Encryption &amp; Audit Architecture</span>
        </h4>
        <p className="text-slate-400 leading-relaxed">
          Cloud credentials are never stored in plaintext. They are encrypted using 256-bit AES-GCM authenticated cipher with unique 96-bit IVs and 128-bit authentication tags. Credentials are never written to logs, serialized into REST DTOs, or exposed via error responses.
        </p>
      </div>

      {/* 1. Add Cloud Account Modal */}
      {createModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.1] shadow-2xl max-w-md w-full space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <h3 className="font-bold text-white text-base">Add New Cloud Account</h3>
              <button onClick={() => setCreateModalOpen(false)} className="text-slate-400 hover:text-white">
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
                  <option value="AWS">Amazon Web Services (AWS Cost Explorer)</option>
                  <option value="AZURE">Microsoft Azure (Azure Cost Management)</option>
                  <option value="GCP">Google Cloud Platform (BigQuery Billing Export)</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">Account / Project Friendly Name</label>
                <input
                  type="text"
                  placeholder="e.g. Production AWS Workloads"
                  value={newAccount.accountName}
                  onChange={(e) => setNewAccount({ ...newAccount, accountName: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">
                  Account / Subscription / Project ID
                </label>
                <input
                  type="text"
                  placeholder="e.g. 123456789012 or gcp-billing-prod"
                  value={newAccount.accountId}
                  onChange={(e) => setNewAccount({ ...newAccount, accountId: e.target.value })}
                  required
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white focus:outline-none focus:border-blue-500 font-mono"
                />
              </div>

              <div>
                <label className="block text-slate-300 mb-1 font-semibold">
                  IAM Role ARN or Service Principal Ref (Optional)
                </label>
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
                  onClick={() => setCreateModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:bg-slate-800 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-md"
                >
                  Save Account
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 2. Connect Credentials Modal */}
      {credModalOpen && selectedAccount && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.1] shadow-2xl max-w-lg w-full space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div>
                <h3 className="font-bold text-white text-base">
                  Connect {selectedAccount.provider} Credentials
                </h3>
                <p className="text-xs text-slate-400">{selectedAccount.accountName}</p>
              </div>
              <button onClick={() => setCredModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Mock / Sandbox Toggle */}
            <div className="p-3 rounded-xl bg-blue-500/10 border border-blue-500/30 text-xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-bold text-blue-300">Sandbox / Mock Provider Mode</span>
                <input
                  type="checkbox"
                  checked={credForm.useMock}
                  onChange={(e) => setCredForm({ ...credForm, useMock: e.target.checked })}
                  className="w-4 h-4 rounded text-blue-600 focus:ring-0 bg-navy-900 border-slate-700"
                />
              </div>
              <p className="text-slate-300 text-[11px] leading-relaxed">
                {credForm.useMock
                  ? 'Simulates actual cloud provider API responses safely without incurring cloud egress charges or requiring production credentials.'
                  : 'Live Provider Mode: Validates against actual Cloud Provider APIs (AWS Cost Explorer, Azure Cost Management, GCP BigQuery). Requires active IAM/RBAC credentials.'}
              </p>
            </div>

            <form onSubmit={handleSaveCredentials} className="space-y-3 text-xs">
              {/* AWS Specific Form */}
              {selectedAccount.provider === 'AWS' && (
                <>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">AWS Access Key ID</label>
                    <input
                      type="text"
                      placeholder="AKIAIOSFODNN7EXAMPLE"
                      value={credForm.accessKeyId}
                      onChange={(e) => setCredForm({ ...credForm, accessKeyId: e.target.value })}
                      required={!credForm.useMock}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">AWS Secret Access Key</label>
                    <input
                      type="password"
                      placeholder="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
                      value={credForm.secretAccessKey}
                      onChange={(e) => setCredForm({ ...credForm, secretAccessKey: e.target.value })}
                      required={!credForm.useMock}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-slate-300 mb-1 font-semibold">IAM Role ARN (Optional)</label>
                      <input
                        type="text"
                        placeholder="arn:aws:iam::..."
                        value={credForm.roleArn}
                        onChange={(e) => setCredForm({ ...credForm, roleArn: e.target.value })}
                        className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-slate-300 mb-1 font-semibold">Region</label>
                      <input
                        type="text"
                        placeholder="us-east-1"
                        value={credForm.region}
                        onChange={(e) => setCredForm({ ...credForm, region: e.target.value })}
                        className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                      />
                    </div>
                  </div>
                </>
              )}

              {/* Azure Specific Form */}
              {selectedAccount.provider === 'AZURE' && (
                <>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">Azure Tenant ID</label>
                    <input
                      type="text"
                      placeholder="00000000-0000-0000-0000-000000000000"
                      value={credForm.tenantId}
                      onChange={(e) => setCredForm({ ...credForm, tenantId: e.target.value })}
                      required={!credForm.useMock}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">Service Principal Client ID</label>
                    <input
                      type="text"
                      placeholder="00000000-0000-0000-0000-000000000000"
                      value={credForm.clientId}
                      onChange={(e) => setCredForm({ ...credForm, clientId: e.target.value })}
                      required={!credForm.useMock}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">Client Secret</label>
                    <input
                      type="password"
                      placeholder="Azure Service Principal Secret"
                      value={credForm.clientSecret}
                      onChange={(e) => setCredForm({ ...credForm, clientSecret: e.target.value })}
                      required={!credForm.useMock}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">Subscription ID</label>
                    <input
                      type="text"
                      placeholder="00000000-0000-0000-0000-000000000000"
                      value={credForm.subscriptionId}
                      onChange={(e) => setCredForm({ ...credForm, subscriptionId: e.target.value })}
                      required={!credForm.useMock}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                    />
                  </div>
                </>
              )}

              {/* GCP Specific Form */}
              {selectedAccount.provider === 'GCP' && (
                <>
                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="block text-slate-300 mb-1 font-semibold">GCP Project ID</label>
                      <input
                        type="text"
                        placeholder="my-gcp-project-id"
                        value={credForm.projectId}
                        onChange={(e) => setCredForm({ ...credForm, projectId: e.target.value })}
                        required={!credForm.useMock}
                        className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                      />
                    </div>
                    <div>
                      <label className="block text-slate-300 mb-1 font-semibold">BigQuery Dataset</label>
                      <input
                        type="text"
                        placeholder="billing_export"
                        value={credForm.datasetId}
                        onChange={(e) => setCredForm({ ...credForm, datasetId: e.target.value })}
                        className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">BigQuery Export Table</label>
                    <input
                      type="text"
                      placeholder="gcp_billing_export_v1_000000_000000_000000"
                      value={credForm.tableName}
                      onChange={(e) => setCredForm({ ...credForm, tableName: e.target.value })}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-slate-300 mb-1 font-semibold">Service Account JSON Key</label>
                    <textarea
                      rows={3}
                      placeholder='{ "type": "service_account", "project_id": "...", ... }'
                      value={credForm.serviceAccountJson}
                      onChange={(e) => setCredForm({ ...credForm, serviceAccountJson: e.target.value })}
                      required={!credForm.useMock}
                      className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono text-[11px] focus:outline-none focus:border-blue-500"
                    />
                  </div>
                </>
              )}

              {/* Test Connection Result */}
              {testResult && (
                <div
                  className={`p-3 rounded-xl border text-xs flex items-start space-x-2 ${
                    testResult.valid
                      ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                      : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
                  }`}
                >
                  {testResult.valid ? (
                    <Check className="w-4 h-4 shrink-0 mt-0.5" />
                  ) : (
                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                  )}
                  <div>
                    <p className="font-semibold">{testResult.message}</p>
                    <p className="text-[10px] text-slate-400 mt-0.5">
                      Status: {testResult.connectionStatus} • Latency: {testResult.latencyMs || 0}ms
                    </p>
                  </div>
                </div>
              )}

              <div className="pt-3 border-t border-slate-800 flex items-center justify-between">
                <button
                  type="button"
                  onClick={handleTestConnection}
                  disabled={testingConnection}
                  className="px-3 py-2 rounded-xl bg-navy-900 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold inline-flex items-center space-x-1.5"
                >
                  {testingConnection ? (
                    <Loader2 className="w-3.5 h-3.5 animate-spin text-blue-400" />
                  ) : (
                    <CheckCircle2 className="w-3.5 h-3.5 text-blue-400" />
                  )}
                  <span>Test Connection</span>
                </button>

                <div className="flex items-center space-x-2">
                  <button
                    type="button"
                    onClick={() => setCredModalOpen(false)}
                    className="px-4 py-2 rounded-xl text-slate-400 hover:bg-slate-800 font-semibold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submittingCreds}
                    className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-md flex items-center space-x-1"
                  >
                    {submittingCreds && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                    <span>Encrypt &amp; Connect</span>
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 3. Sync Audit History Modal */}
      {historyModalOpen && selectedAccount && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.1] shadow-2xl max-w-3xl w-full space-y-4 max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div>
                <h3 className="font-bold text-white text-base">Synchronization Audit Logs</h3>
                <p className="text-xs text-slate-400 font-mono">
                  {selectedAccount.provider} • {selectedAccount.accountName} ({selectedAccount.accountId})
                </p>
              </div>
              <button onClick={() => setHistoryModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            {historyLoading ? (
              <div className="py-12 text-center text-slate-400">
                <Loader2 className="w-8 h-8 animate-spin mx-auto text-blue-400 mb-2" />
                <p className="text-xs font-mono">Loading sync history logs...</p>
              </div>
            ) : syncHistory.length === 0 ? (
              <div className="py-12 text-center text-slate-400 space-y-2">
                <History className="w-10 h-10 mx-auto text-slate-600" />
                <p className="font-semibold text-sm">No synchronization history found for this account.</p>
                <p className="text-xs text-slate-500">Trigger a sync using "Sync Now" to generate an audit run.</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse font-mono">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400 text-[11px]">
                      <th className="pb-2">Timestamp</th>
                      <th className="pb-2">Status</th>
                      <th className="pb-2 text-right">Fetched</th>
                      <th className="pb-2 text-right">Inserted</th>
                      <th className="pb-2 text-right">Skipped (Idempotent)</th>
                      <th className="pb-2">Billing Range</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60">
                    {syncHistory.map((item) => (
                      <tr key={item.id} className="hover:bg-slate-800/30">
                        <td className="py-2.5 text-slate-300">
                          {new Date(item.syncStartTime).toLocaleString()}
                        </td>
                        <td className="py-2.5">
                          {item.syncStatus === 'SUCCESS' ? (
                            <span className="text-emerald-400 font-semibold px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20 text-[10px]">
                              SUCCESS
                            </span>
                          ) : (
                            <span className="text-rose-400 font-semibold px-2 py-0.5 rounded bg-rose-500/10 border border-rose-500/20 text-[10px]">
                              FAILED
                            </span>
                          )}
                        </td>
                        <td className="py-2.5 text-right font-bold text-white">{item.recordsFetched}</td>
                        <td className="py-2.5 text-right font-bold text-emerald-400">
                          +{item.recordsInserted}
                        </td>
                        <td className="py-2.5 text-right text-slate-400">
                          {item.recordsSkipped}
                        </td>
                        <td className="py-2.5 text-slate-400 text-[11px]">
                          {item.dateRangeStart} to {item.dateRangeEnd}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="pt-3 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setHistoryModalOpen(false)}
                className="px-4 py-2 rounded-xl bg-navy-900 border border-slate-800 hover:border-slate-700 text-slate-200 text-xs font-semibold"
              >
                Close Audit Logs
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
