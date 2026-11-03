import apiClient from './api';

export const anomalyService = {
  async getAnomalies(params = {}) {
    const response = await apiClient.get('/api/anomalies', { params });
    return response.data?.data || [];
  },

  async getAnomalyById(id) {
    const response = await apiClient.get(`/api/anomalies/${id}`);
    return response.data?.data;
  },

  async updateAnomalyStatus(id, status) {
    const response = await apiClient.patch(`/api/anomalies/${id}/status`, { status });
    return response.data?.data;
  },
};

export default anomalyService;
