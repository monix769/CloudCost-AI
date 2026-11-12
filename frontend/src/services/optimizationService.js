import apiClient from './api';

export const optimizationService = {
  async getOverview() {
    const response = await apiClient.get('/api/optimization/overview');
    return response.data?.data;
  },

  async getRecommendations(params = {}) {
    const response = await apiClient.get('/api/optimization/recommendations', { params });
    return response.data?.data;
  },

  async getRecommendationById(id) {
    const response = await apiClient.get(`/api/optimization/recommendations/${id}`);
    return response.data?.data;
  },

  async updateStatus(id, status, notes = null, realizedMonthlySavings = null) {
    const payload = { status };
    if (notes) payload.notes = notes;
    if (realizedMonthlySavings !== null && realizedMonthlySavings !== undefined) {
      payload.realizedMonthlySavings = realizedMonthlySavings;
    }
    const response = await apiClient.patch(`/api/optimization/recommendations/${id}/status`, payload);
    return response.data?.data;
  },

  async getRecommendationHistory(id) {
    const response = await apiClient.get(`/api/optimization/recommendations/${id}/history`);
    return response.data?.data || [];
  },

  async getSavingsSummary() {
    const response = await apiClient.get('/api/optimization/savings-summary');
    return response.data?.data;
  },

  async getResourceUtilization(params = {}) {
    const response = await apiClient.get('/api/optimization/resource-utilization', { params });
    return response.data?.data;
  },

  async getCommitmentAnalysis() {
    const response = await apiClient.get('/api/optimization/commitment-analysis');
    return response.data?.data;
  },

  async runOptimizationAnalysis() {
    const response = await apiClient.post('/api/optimization/analyze');
    return response.data?.data;
  },

  async exportCsv() {
    const response = await apiClient.get('/api/optimization/export', {
      responseType: 'blob'
    });
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', 'cost-optimization-recommendations.csv');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
};

