import apiClient from './api';

export const anomalyService = {
  async getAnomalies(params = {}) {
    const response = await apiClient.get('/api/anomalies', { params });
    return response.data?.data;
  },

  async getAnomalyById(id) {
    const response = await apiClient.get(`/api/anomalies/${id}`);
    return response.data?.data;
  },

  async getAnomalySummary(params = {}) {
    const response = await apiClient.get('/api/anomalies/summary', { params });
    return response.data?.data;
  },

  async getAnomalyTrends(params = {}) {
    const response = await apiClient.get('/api/anomalies/trends', { params });
    return response.data?.data;
  },

  async triggerDetection(targetDate) {
    const params = targetDate ? { targetDate } : {};
    const response = await apiClient.post('/api/anomalies/detect', null, { params });
    return response.data?.data;
  },

  async updateAnomalyStatus(id, status, notes = null) {
    const payload = { status };
    if (notes) payload.notes = notes;
    const response = await apiClient.patch(`/api/anomalies/${id}/status`, payload);
    return response.data?.data;
  },

  async getAnomalyHistory(id) {
    const response = await apiClient.get(`/api/anomalies/${id}/history`);
    return response.data?.data || [];
  },

  async getDetectionConfig() {
    const response = await apiClient.get('/api/anomalies/config');
    return response.data?.data;
  },

  async updateDetectionConfig(config) {
    const response = await apiClient.put('/api/anomalies/config', config);
    return response.data?.data;
  },
};

export default anomalyService;
