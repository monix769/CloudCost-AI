import apiClient from './api';

/**
 * System and platform diagnostic API service methods.
 */
export const systemService = {
  /**
   * Fetch root platform information and endpoint discovery links.
   */
  async getRootInfo() {
    const response = await apiClient.get('/');
    return response.data;
  },

  /**
   * Probe application and MySQL database operational health.
   */
  async getHealth() {
    const response = await apiClient.get('/api/health');
    return response.data;
  },

  /**
   * Fetch detailed architectural and runtime environment status.
   */
  async getStatus() {
    const response = await apiClient.get('/api/status');
    return response.data;
  },
};
