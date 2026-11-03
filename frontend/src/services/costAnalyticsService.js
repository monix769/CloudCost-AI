import apiClient from './api';

export const costAnalyticsService = {
  async getCostSummary(params = {}) {
    const response = await apiClient.get('/api/costs/summary', { params });
    return response.data?.data;
  },

  async getCostsByProvider(params = {}) {
    const response = await apiClient.get('/api/costs/by-provider', { params });
    return response.data?.data || [];
  },

  async getCostsByService(params = {}) {
    const response = await apiClient.get('/api/costs/by-service', { params });
    return response.data?.data || [];
  },

  async getCostTrends(params = {}) {
    const response = await apiClient.get('/api/costs/trends', { params });
    return response.data?.data || [];
  },
};

export default costAnalyticsService;
