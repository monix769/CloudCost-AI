import apiClient from './api';

export const organizationManagementService = {
  /**
   * Get current organization profile and member statistics.
   */
  async getProfile() {
    const response = await apiClient.get('/api/organizations/current');
    return response.data?.data;
  },

  /**
   * Update organization name or settings (Admin only).
   * @param {Object} payload - { name }
   */
  async updateProfile(payload) {
    const response = await apiClient.put('/api/organizations/current', payload);
    return response.data?.data;
  },

  /**
   * List all members of the organization.
   */
  async getMembers() {
    const response = await apiClient.get('/api/organizations/current/members');
    return response.data?.data || [];
  },

  /**
   * Update role or active status of a team member (Admin only).
   * @param {number|string} memberId
   * @param {Object} payload - { role, isActive }
   */
  async updateMember(memberId, payload) {
    const response = await apiClient.put(`/api/organizations/current/members/${memberId}`, payload);
    return response.data?.data;
  },

  /**
   * Remove a member from the organization (Admin only).
   * @param {number|string} memberId
   */
  async removeMember(memberId) {
    const response = await apiClient.delete(`/api/organizations/current/members/${memberId}`);
    return response.data;
  },

  /**
   * Send an invitation to a new member (Admin only).
   * @param {Object} payload - { email, role }
   */
  async inviteMember(payload) {
    const response = await apiClient.post('/api/organizations/current/invitations', payload);
    return response.data?.data;
  },

  /**
   * List pending and historical invitations (Admin only).
   */
  async getInvitations() {
    const response = await apiClient.get('/api/organizations/current/invitations');
    return response.data?.data || [];
  },

  /**
   * Revoke a pending invitation (Admin only).
   * @param {number|string} invitationId
   */
  async revokeInvitation(invitationId) {
    const response = await apiClient.delete(`/api/organizations/current/invitations/${invitationId}`);
    return response.data;
  },
};

export default organizationManagementService;

