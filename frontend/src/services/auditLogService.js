import apiClient from './api';

export const auditLogService = {
  /**
   * Query enterprise audit logs with filters and pagination.
   * @param {Object} params - { action, userId, startDate, endDate, page, size }
   */
  async getAuditLogs(params = {}) {
    const response = await apiClient.get('/api/audit-logs', { params });
    return response.data?.data;
  },

  /**
   * Get audit log detail by ID.
   * @param {number|string} id
   */
  async getAuditLogById(id) {
    const response = await apiClient.get(`/api/audit-logs/${id}`);
    return response.data?.data;
  },
};

export default auditLogService;

