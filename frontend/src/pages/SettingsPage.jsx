import React, { useState, useEffect, useCallback } from 'react';
import {
  Settings,
  User,
  Shield,
  Key,
  Globe,
  Bell,
  Users,
  FileSpreadsheet,
  Check,
  AlertCircle,
  CheckCircle,
  X,
  Mail,
  UserPlus,
  RefreshCw,
  Search,
  Filter,
  Trash2,
  Copy,
  Clock,
  Activity,
  ChevronDown,
  Building,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import organizationManagementService from '../services/organizationManagementService';
import auditLogService from '../services/auditLogService';

export default function SettingsPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  // Navigation tab
  const [activeTab, setActiveTab] = useState('organization'); // 'organization', 'members', 'audit', 'preferences'

  // Organization state
  const [orgProfile, setOrgProfile] = useState(null);
  const [orgNameInput, setOrgNameInput] = useState('');
  const [savingOrg, setSavingOrg] = useState(false);

  // Members & Invitations state
  const [members, setMembers] = useState([]);
  const [invitations, setInvitations] = useState([]);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('USER');
  const [sendingInvite, setSendingInvite] = useState(false);

  // Audit Logs state
  const [auditLogs, setAuditLogs] = useState([]);
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditActionFilter, setAuditActionFilter] = useState('');
  const [auditSearchUser, setAuditSearchUser] = useState('');
  const [expandedLogId, setExpandedLogId] = useState(null);

  // Notification / Toast state
  const [statusMessage, setStatusMessage] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);

  // Preferences (Saved in localStorage / state)
  const [currency, setCurrency] = useState('USD');
  const [fiscalYear, setFiscalYear] = useState('CALENDAR');

  // Load Org Profile
  const fetchOrgProfile = useCallback(async () => {
    try {
      const data = await organizationManagementService.getProfile();
      setOrgProfile(data);
      if (data) setOrgNameInput(data.name || '');
    } catch (err) {
      console.error('Failed to load organization profile:', err);
    }
  }, []);

  // Load Members & Invitations
  const fetchMembersAndInvitations = useCallback(async () => {
    try {
      setLoadingMembers(true);
      const [membersData, invitesData] = await Promise.all([
        organizationManagementService.getMembers(),
        isAdmin ? organizationManagementService.getInvitations() : Promise.resolve([]),
      ]);
      setMembers(membersData || []);
      setInvitations(invitesData || []);
    } catch (err) {
      console.error('Failed to load team data:', err);
    } finally {
      setLoadingMembers(false);
    }
  }, [isAdmin]);

  // Load Audit Logs
  const fetchAuditLogs = useCallback(async () => {
    if (!isAdmin) return;
    try {
      setAuditLoading(true);
      const params = {};
      if (auditActionFilter) params.action = auditActionFilter;
      const response = await auditLogService.getAuditLogs(params);
      const items = response?.content || response || [];
      setAuditLogs(Array.isArray(items) ? items : []);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
    } finally {
      setAuditLoading(false);
    }
  }, [isAdmin, auditActionFilter]);

  useEffect(() => {
    fetchOrgProfile();
    fetchMembersAndInvitations();
    if (isAdmin) {
      fetchAuditLogs();
    }
  }, [fetchOrgProfile, fetchMembersAndInvitations, fetchAuditLogs, isAdmin]);

  // Handlers
  const handleSaveOrgName = async (e) => {
    e.preventDefault();
    if (!isAdmin) return;
    try {
      setSavingOrg(true);
      const updated = await organizationManagementService.updateProfile({ name: orgNameInput });
      setOrgProfile(updated);
      setStatusMessage('Organization name updated successfully.');
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error('Failed to update organization name:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to update organization name');
    } finally {
      setSavingOrg(false);
    }
  };

  const handleRoleChange = async (memberId, newRole) => {
    try {
      await organizationManagementService.updateMember(memberId, { role: newRole });
      setStatusMessage('Member role updated.');
      fetchMembersAndInvitations();
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error('Role update failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to update member role');
    }
  };

  const handleStatusToggle = async (member) => {
    try {
      await organizationManagementService.updateMember(member.id, { isActive: !member.isActive });
      setStatusMessage(`Member ${member.isActive ? 'deactivated' : 'activated'}.`);
      fetchMembersAndInvitations();
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error('Status toggle failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to update member status');
    }
  };

  const handleRemoveMember = async (member) => {
    if (!window.confirm(`Are you sure you want to remove ${member.firstName} ${member.lastName} (${member.email}) from the organization?`)) {
      return;
    }
    try {
      await organizationManagementService.removeMember(member.id);
      setStatusMessage(`Member ${member.email} removed.`);
      fetchMembersAndInvitations();
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error('Remove member failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to remove member');
    }
  };

  const handleSendInvite = async (e) => {
    e.preventDefault();
    try {
      setSendingInvite(true);
      await organizationManagementService.inviteMember({
        email: inviteEmail,
        role: inviteRole,
      });
      setStatusMessage(`Invitation dispatched to ${inviteEmail}`);
      setShowInviteModal(false);
      setInviteEmail('');
      setInviteRole('USER');
      fetchMembersAndInvitations();
      setTimeout(() => setStatusMessage(null), 4000);
    } catch (err) {
      console.error('Send invite failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to send invitation');
    } finally {
      setSendingInvite(false);
    }
  };

  const handleRevokeInvite = async (invitationId, email) => {
    if (!window.confirm(`Revoke pending invitation for ${email}?`)) return;
    try {
      await organizationManagementService.revokeInvitation(invitationId);
      setStatusMessage(`Invitation revoked.`);
      fetchMembersAndInvitations();
      setTimeout(() => setStatusMessage(null), 3000);
    } catch (err) {
      console.error('Revoke invite failed:', err);
      setErrorMessage(err.response?.data?.message || 'Failed to revoke invitation');
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setStatusMessage('Invitation token copied to clipboard.');
    setTimeout(() => setStatusMessage(null), 2500);
  };

  const filteredAuditLogs = auditLogs.filter((log) => {
    if (!auditSearchUser) return true;
    return log.userEmail?.toLowerCase().includes(auditSearchUser.toLowerCase());
  });

  return (
    <div className="space-y-6 animate-fadeIn max-w-6xl">
      {/* Toast notifications */}
      {statusMessage && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>{statusMessage}</span>
          </div>
          <button onClick={() => setStatusMessage(null)} className="hover:text-emerald-200">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button onClick={() => setErrorMessage(null)} className="hover:text-rose-200">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Header */}
      <div className="pb-4 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center space-x-2">
            <Settings className="w-5 h-5 text-blue-400" />
            <span>Enterprise Settings &amp; Governance</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Manage organization tenancy, team access control, enterprise audit trails, and global FinOps standards
          </p>
        </div>

        <div className="flex items-center space-x-2 self-start sm:self-auto">
          <span className="text-[11px] font-mono px-2.5 py-1 rounded-full bg-blue-500/10 text-blue-300 border border-blue-500/20 font-semibold flex items-center space-x-1.5">
            <Shield className="w-3.5 h-3.5" />
            <span>Role: {user?.role || 'USER'}</span>
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-800 space-x-1 overflow-x-auto text-xs">
        <button
          onClick={() => setActiveTab('organization')}
          className={`py-2.5 px-4 font-semibold border-b-2 flex items-center space-x-2 transition-all whitespace-nowrap ${
            activeTab === 'organization'
              ? 'border-blue-500 text-blue-400 bg-blue-500/5'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Building className="w-4 h-4" />
          <span>Organization Profile</span>
        </button>

        <button
          onClick={() => setActiveTab('members')}
          className={`py-2.5 px-4 font-semibold border-b-2 flex items-center space-x-2 transition-all whitespace-nowrap ${
            activeTab === 'members'
              ? 'border-blue-500 text-blue-400 bg-blue-500/5'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Team &amp; Access Control</span>
          <span className="px-1.5 py-0.2 rounded-full bg-slate-800 text-[10px] text-slate-300">{members.length}</span>
        </button>

        {isAdmin && (
          <button
            onClick={() => setActiveTab('audit')}
            className={`py-2.5 px-4 font-semibold border-b-2 flex items-center space-x-2 transition-all whitespace-nowrap ${
              activeTab === 'audit'
                ? 'border-blue-500 text-blue-400 bg-blue-500/5'
                : 'border-transparent text-slate-400 hover:text-slate-200'
            }`}
          >
            <Activity className="w-4 h-4" />
            <span>Enterprise Audit Log</span>
          </button>
        )}

        <button
          onClick={() => setActiveTab('preferences')}
          className={`py-2.5 px-4 font-semibold border-b-2 flex items-center space-x-2 transition-all whitespace-nowrap ${
            activeTab === 'preferences'
              ? 'border-blue-500 text-blue-400 bg-blue-500/5'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Globe className="w-4 h-4" />
          <span>Platform Preferences</span>
        </button>
      </div>

      {/* Tab 1: Organization Profile */}
      {activeTab === 'organization' && (
        <div className="space-y-6 animate-fadeIn">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <Building className="w-4 h-4 text-blue-400" />
                <h3 className="font-bold text-white text-sm">Tenant Organization Details</h3>
              </div>
              <span className="text-[10px] font-mono px-2.5 py-1 rounded-full bg-purple-500/10 text-purple-300 border border-purple-500/20 font-bold uppercase">
                Tier: {orgProfile?.tier || 'ENTERPRISE'}
              </span>
            </div>

            <form onSubmit={handleSaveOrgName} className="space-y-4 text-xs">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-slate-400 mb-1 font-semibold">Organization Legal Name</label>
                  <input
                    type="text"
                    value={orgNameInput}
                    onChange={(e) => setOrgNameInput(e.target.value)}
                    disabled={!isAdmin || savingOrg}
                    className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-semibold focus:outline-none focus:border-blue-500 disabled:opacity-60"
                  />
                  {!isAdmin && <p className="text-[10px] text-slate-500 mt-1">Only Organization Admins can rename the organization.</p>}
                </div>

                <div>
                  <label className="block text-slate-400 mb-1 font-semibold">Tenant Slug (Immutable Key)</label>
                  <input
                    type="text"
                    value={orgProfile?.slug || ''}
                    disabled
                    className="w-full p-2.5 rounded-xl bg-navy-950 border border-slate-800 text-slate-400 font-mono"
                  />
                  <p className="text-[10px] text-slate-500 mt-1">Multi-tenant database isolation partition identifier.</p>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-2">
                <div className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800">
                  <span className="text-slate-400 text-[11px] block">Active Member Count</span>
                  <span className="text-lg font-bold text-white mt-1 block">{orgProfile?.totalMembers || members.length} Members</span>
                </div>
                <div className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800">
                  <span className="text-slate-400 text-[11px] block">Tenant Status</span>
                  <span className="text-emerald-400 font-bold text-sm mt-1 inline-flex items-center space-x-1.5">
                    <CheckCircle className="w-3.5 h-3.5" />
                    <span>Active &amp; Compliant</span>
                  </span>
                </div>
                <div className="p-3.5 rounded-xl bg-navy-900/60 border border-slate-800">
                  <span className="text-slate-400 text-[11px] block">Provisioned Since</span>
                  <span className="text-slate-300 font-mono text-xs mt-1 block">
                    {orgProfile?.createdAt ? new Date(orgProfile.createdAt).toLocaleDateString() : 'N/A'}
                  </span>
                </div>
              </div>

              {isAdmin && (
                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={savingOrg}
                    className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-lg shadow-blue-500/20 transition-all flex items-center space-x-2"
                  >
                    {savingOrg ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                    <span>Save Changes</span>
                  </button>
                </div>
              )}
            </form>
          </div>
        </div>
      )}

      {/* Tab 2: Team Members & Invitations */}
      {activeTab === 'members' && (
        <div className="space-y-6 animate-fadeIn">
          {/* Members Table */}
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
              <div>
                <h3 className="font-bold text-white text-sm flex items-center space-x-2">
                  <Users className="w-4 h-4 text-blue-400" />
                  <span>Organization Members</span>
                  <span className="text-xs font-mono font-normal text-slate-400">({members.length})</span>
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">Control tenant access rights and manage FinOps administration privileges</p>
              </div>

              {isAdmin && (
                <button
                  onClick={() => setShowInviteModal(true)}
                  className="inline-flex items-center space-x-2 px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all self-start sm:self-auto"
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  <span>Invite New Member</span>
                </button>
              )}
            </div>

            {loadingMembers ? (
              <div className="py-8 flex justify-center">
                <RefreshCw className="w-5 h-5 text-blue-400 animate-spin" />
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400 font-mono uppercase text-[11px]">
                      <th className="py-3 px-4">Member</th>
                      <th className="py-3 px-3">Role</th>
                      <th className="py-3 px-3">Status</th>
                      <th className="py-3 px-3">Last Active</th>
                      {isAdmin && <th className="py-3 px-4 text-right">Actions</th>}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/40">
                    {members.map((m) => (
                      <tr key={m.id} className="hover:bg-white/[0.02] transition-colors">
                        <td className="py-3 px-4">
                          <div className="font-semibold text-white">
                            {m.firstName} {m.lastName} {user?.email === m.email && <span className="text-blue-400 text-[10px]">(You)</span>}
                          </div>
                          <div className="text-[11px] font-mono text-slate-400">{m.email}</div>
                        </td>
                        <td className="py-3 px-3">
                          {isAdmin && user?.id !== m.id ? (
                            <select
                              value={m.role}
                              onChange={(e) => handleRoleChange(m.id, e.target.value)}
                              className="px-2 py-1 rounded-lg bg-navy-900 border border-slate-800 text-xs text-white font-mono focus:outline-none focus:border-blue-500"
                            >
                              <option value="ADMIN">ADMIN</option>
                              <option value="USER">USER</option>
                            </select>
                          ) : (
                            <span
                              className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
                                m.role === 'ADMIN'
                                  ? 'bg-purple-500/10 text-purple-300 border border-purple-500/20'
                                  : 'bg-slate-700/50 text-slate-300 border border-slate-600/30'
                              }`}
                            >
                              {m.role}
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-3">
                          {m.isActive ? (
                            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 font-bold">
                              ACTIVE
                            </span>
                          ) : (
                            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-rose-500/10 text-rose-300 border border-rose-500/20 font-bold">
                              DEACTIVATED
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-3 font-mono text-slate-400 text-[11px] whitespace-nowrap">
                          {m.lastLoginAt ? new Date(m.lastLoginAt).toLocaleString() : 'Never'}
                        </td>
                        {isAdmin && (
                          <td className="py-3 px-4 text-right whitespace-nowrap">
                            {user?.id !== m.id && (
                              <div className="flex items-center justify-end space-x-2">
                                <button
                                  onClick={() => handleStatusToggle(m)}
                                  className="text-xs text-slate-400 hover:text-white px-2 py-1 rounded bg-navy-800 border border-slate-700 transition-colors"
                                  title={m.isActive ? 'Deactivate account' : 'Reactivate account'}
                                >
                                  {m.isActive ? 'Deactivate' : 'Activate'}
                                </button>
                                <button
                                  onClick={() => handleRemoveMember(m)}
                                  className="p-1 rounded text-slate-500 hover:text-rose-400 transition-colors"
                                  title="Remove from organization"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              </div>
                            )}
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Pending Invitations Section (Admin Only) */}
          {isAdmin && (
            <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center space-x-2">
                  <Mail className="w-4 h-4 text-blue-400" />
                  <h3 className="font-bold text-white text-sm">Pending &amp; Historical Invitations</h3>
                  <span className="text-xs font-mono font-normal text-slate-400">({invitations.length})</span>
                </div>
              </div>

              {invitations.length === 0 ? (
                <p className="text-xs text-slate-500 py-3">No pending or recent team invitations.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="border-b border-slate-800 text-slate-400 font-mono uppercase text-[11px]">
                        <th className="py-2.5 px-4">Invited Email</th>
                        <th className="py-2.5 px-3">Role</th>
                        <th className="py-2.5 px-3">Status</th>
                        <th className="py-2.5 px-3">Expires At</th>
                        <th className="py-2.5 px-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800/40">
                      {invitations.map((inv) => (
                        <tr key={inv.id} className="hover:bg-white/[0.02]">
                          <td className="py-2.5 px-4 font-mono text-slate-200">{inv.email}</td>
                          <td className="py-2.5 px-3 font-mono text-slate-300">{inv.role}</td>
                          <td className="py-2.5 px-3">
                            <span
                              className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded ${
                                inv.status === 'PENDING'
                                  ? 'bg-amber-500/10 text-amber-300 border border-amber-500/20'
                                  : inv.status === 'ACCEPTED'
                                  ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                                  : 'bg-slate-700/40 text-slate-400 border border-slate-600/30'
                              }`}
                            >
                              {inv.status}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 font-mono text-slate-400 text-[11px]">
                            {new Date(inv.expiresAt).toLocaleDateString()}
                          </td>
                          <td className="py-2.5 px-4 text-right">
                            {inv.status === 'PENDING' && (
                              <div className="flex items-center justify-end space-x-2">
                                <button
                                  onClick={() => copyToClipboard(inv.invitationToken)}
                                  className="text-[11px] text-blue-400 hover:text-blue-300 inline-flex items-center space-x-1"
                                  title="Copy invite token"
                                >
                                  <Copy className="w-3 h-3" />
                                  <span>Token</span>
                                </button>
                                <button
                                  onClick={() => handleRevokeInvite(inv.id, inv.email)}
                                  className="text-[11px] text-rose-400 hover:text-rose-300"
                                  title="Revoke invitation"
                                >
                                  Revoke
                                </button>
                              </div>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Enterprise Audit Log (Admin Only) */}
      {activeTab === 'audit' && isAdmin && (
        <div className="space-y-6 animate-fadeIn">
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800">
              <div>
                <h3 className="font-bold text-white text-sm flex items-center space-x-2">
                  <Activity className="w-4 h-4 text-blue-400" />
                  <span>Immutable Audit Trail</span>
                  <span className="text-xs font-mono font-normal text-slate-400">({filteredAuditLogs.length} events)</span>
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">Enterprise security log recording identity actions, access events, and report downloads</p>
              </div>

              <button
                onClick={() => fetchAuditLogs()}
                disabled={auditLoading}
                className="p-2 rounded-xl border border-slate-800 bg-navy-900 hover:bg-navy-800 text-slate-300 transition-colors self-start sm:self-auto"
                title="Refresh audit trail"
              >
                <RefreshCw className={`w-4 h-4 ${auditLoading ? 'animate-spin' : ''}`} />
              </button>
            </div>

            {/* Filters */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-3" />
                <input
                  type="text"
                  placeholder="Filter by user email..."
                  value={auditSearchUser}
                  onChange={(e) => setAuditSearchUser(e.target.value)}
                  className="w-full pl-8 pr-3 py-2 rounded-xl bg-navy-900 border border-slate-800 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 font-mono text-xs"
                />
              </div>

              <div>
                <select
                  value={auditActionFilter}
                  onChange={(e) => setAuditActionFilter(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500 text-xs"
                >
                  <option value="">All Audit Actions</option>
                  <option value="LOGIN">LOGIN</option>
                  <option value="LOGOUT">LOGOUT</option>
                  <option value="FAILED_LOGIN">FAILED_LOGIN</option>
                  <option value="USER_REGISTER">USER_REGISTER</option>
                  <option value="MEMBER_INVITED">MEMBER_INVITED</option>
                  <option value="MEMBER_ROLE_UPDATED">MEMBER_ROLE_UPDATED</option>
                  <option value="MEMBER_STATUS_UPDATED">MEMBER_STATUS_UPDATED</option>
                  <option value="MEMBER_REMOVED">MEMBER_REMOVED</option>
                  <option value="REPORT_GENERATED">REPORT_GENERATED</option>
                  <option value="REPORT_DOWNLOADED">REPORT_DOWNLOADED</option>
                  <option value="REPORT_DELETED">REPORT_DELETED</option>
                  <option value="ORGANIZATION_UPDATED">ORGANIZATION_UPDATED</option>
                </select>
              </div>
            </div>

            {/* Audit Log Table */}
            {auditLoading ? (
              <div className="py-8 flex justify-center">
                <RefreshCw className="w-5 h-5 text-blue-400 animate-spin" />
              </div>
            ) : filteredAuditLogs.length === 0 ? (
              <p className="text-xs text-slate-500 py-6 text-center">No audit log records match the current filters.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-800 text-slate-400 font-mono uppercase text-[11px]">
                      <th className="py-3 px-4">Timestamp</th>
                      <th className="py-3 px-3">Actor / User</th>
                      <th className="py-3 px-3">Action</th>
                      <th className="py-3 px-3">IP Address</th>
                      <th className="py-3 px-3">Status</th>
                      <th className="py-3 px-4 text-right">Details</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/40">
                    {filteredAuditLogs.map((log) => (
                      <React.Fragment key={log.id}>
                        <tr className="hover:bg-white/[0.02] transition-colors">
                          <td className="py-2.5 px-4 font-mono text-slate-400 text-[11px] whitespace-nowrap">
                            {new Date(log.timestamp).toLocaleString()}
                          </td>
                          <td className="py-2.5 px-3 font-semibold text-white max-w-xs truncate">
                            {log.userEmail || 'System / Unauthenticated'}
                          </td>
                          <td className="py-2.5 px-3">
                            <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 border border-blue-500/20 font-bold">
                              {log.action}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 font-mono text-slate-400 text-[11px]">
                            {log.ipAddress || '—'}
                          </td>
                          <td className="py-2.5 px-3">
                            {log.status === 'SUCCESS' ? (
                              <span className="text-[10px] font-mono text-emerald-400 font-bold">SUCCESS</span>
                            ) : (
                              <span className="text-[10px] font-mono text-rose-400 font-bold">FAILED</span>
                            )}
                          </td>
                          <td className="py-2.5 px-4 text-right">
                            {log.details && (
                              <button
                                onClick={() => setExpandedLogId(expandedLogId === log.id ? null : log.id)}
                                className="text-blue-400 hover:text-blue-300 font-mono text-[11px]"
                              >
                                {expandedLogId === log.id ? 'Hide' : 'View'}
                              </button>
                            )}
                          </td>
                        </tr>
                        {expandedLogId === log.id && (
                          <tr className="bg-navy-950/80">
                            <td colSpan={6} className="py-3 px-4">
                              <pre className="text-[11px] font-mono text-slate-300 bg-navy-900 p-3 rounded-xl border border-slate-800 overflow-x-auto whitespace-pre-wrap">
                                {log.details}
                              </pre>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 4: Platform Preferences */}
      {activeTab === 'preferences' && (
        <div className="space-y-6 animate-fadeIn">
          {/* Currency Standards */}
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
            <div className="flex items-center space-x-2 pb-3 border-b border-slate-800">
              <Globe className="w-4 h-4 text-blue-400" />
              <h3 className="font-bold text-white text-sm">Financial Presentation Standards</h3>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
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
                  <option value="CAD">CAD (C$) — Canadian Dollar</option>
                </select>
              </div>

              <div>
                <label className="block text-slate-400 mb-1 font-semibold">Fiscal Year Calendar</label>
                <select
                  value={fiscalYear}
                  onChange={(e) => setFiscalYear(e.target.value)}
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                >
                  <option value="CALENDAR">Standard Calendar Year (Jan - Dec)</option>
                  <option value="APRIL">April to March Fiscal</option>
                  <option value="OCTOBER">October to September Fiscal</option>
                </select>
              </div>
            </div>
          </div>

          {/* Webhook Notifications */}
          <div className="glass-panel rounded-2xl p-6 border border-white/[0.08] shadow-card-elevated space-y-4">
            <div className="flex items-center space-x-2 pb-3 border-b border-slate-800">
              <Bell className="w-4 h-4 text-blue-400" />
              <h3 className="font-bold text-white text-sm">Alert Routing Endpoints</h3>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-400 mb-1 font-semibold">Slack Incoming Webhook URL</label>
                <input
                  type="text"
                  placeholder="https://hooks.slack.com/services/..."
                  defaultValue="https://hooks.slack.com/services/T00/B00/XXXXX"
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-slate-300 font-mono focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="flex items-center space-x-3 pt-2">
                <input
                  type="checkbox"
                  id="emailAlerts"
                  defaultChecked
                  className="rounded bg-navy-900 border-slate-800 text-blue-600 focus:ring-blue-500 h-4 w-4"
                />
                <label htmlFor="emailAlerts" className="text-slate-300 font-semibold cursor-pointer">
                  Send email notifications on CRITICAL and HIGH cost anomalies
                </label>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Invite Member Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="glass-panel w-full max-w-md rounded-2xl border border-white/[0.12] p-6 shadow-2xl space-y-4 bg-navy-950">
            <div className="flex items-center justify-between pb-3 border-b border-slate-800">
              <div className="flex items-center space-x-2">
                <UserPlus className="w-5 h-5 text-blue-400" />
                <h3 className="font-bold text-white text-base">Invite Team Member</h3>
              </div>
              <button
                onClick={() => setShowInviteModal(false)}
                className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-white/10"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSendInvite} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Corporate Email Address</label>
                <input
                  type="email"
                  required
                  placeholder="colleague@company.com"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white placeholder-slate-500 font-mono focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Assigned Tenant Role</label>
                <select
                  value={inviteRole}
                  onChange={(e) => setInviteRole(e.target.value)}
                  className="w-full p-2.5 rounded-xl bg-navy-900 border border-slate-800 text-white font-mono focus:outline-none focus:border-blue-500"
                >
                  <option value="USER">USER — Read-only / Analyst access to billing &amp; reports</option>
                  <option value="ADMIN">ADMIN — Full FinOps management &amp; tenant administration</option>
                </select>
              </div>

              <div className="pt-3 border-t border-slate-800 flex items-center justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setShowInviteModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white hover:bg-white/5 transition-colors font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={sendingInvite}
                  className="px-5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-semibold shadow-lg shadow-blue-500/20 transition-all flex items-center space-x-2"
                >
                  {sendingInvite ? (
                    <>
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      <span>Sending Invitation...</span>
                    </>
                  ) : (
                    <>
                      <Mail className="w-4 h-4" />
                      <span>Send Invitation</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
