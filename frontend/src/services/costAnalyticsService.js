import apiClient from './api';

export const costAnalyticsService = {
  // Legacy M3/M5 endpoints (preserved for full backward compatibility)
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

  // Milestone 6 - Advanced FinOps Analytics Endpoints
  async getOverview(params = {}) {
    const response = await apiClient.get('/api/analytics/overview', { params });
    return response.data?.data;
  },

  async getSpendingTrends(params = {}) {
    const response = await apiClient.get('/api/analytics/spending-trends', { params });
    return response.data?.data;
  },

  async getProviderComparison(params = {}) {
    const response = await apiClient.get('/api/analytics/provider-comparison', { params });
    return response.data?.data || [];
  },

  async getServiceBreakdown(params = {}) {
    const response = await apiClient.get('/api/analytics/service-breakdown', { params });
    return response.data?.data || [];
  },

  async getTopSpendingServices(params = {}) {
    const response = await apiClient.get('/api/analytics/top-spending-services', { params });
    return response.data?.data || [];
  },

  async getAccountBreakdown(params = {}) {
    const response = await apiClient.get('/api/analytics/account-breakdown', { params });
    return response.data?.data || [];
  },

  async getTopSpendingAccounts(params = {}) {
    const response = await apiClient.get('/api/analytics/top-spending-accounts', { params });
    return response.data?.data || [];
  },

  async getMonthOverMonth(params = {}) {
    const response = await apiClient.get('/api/analytics/month-over-month', { params });
    return response.data?.data;
  },

  async getForecast(params = {}) {
    const response = await apiClient.get('/api/analytics/forecast', { params });
    return response.data?.data;
  },

  async getBudgetUtilization(params = {}) {
    const response = await apiClient.get('/api/analytics/budget-utilization', { params });
    return response.data?.data;
  },

  async getCostAllocation(params = {}) {
    const response = await apiClient.get('/api/analytics/cost-allocation', { params });
    return response.data?.data;
  },

  async getInsights(params = {}) {
    const response = await apiClient.get('/api/analytics/insights', { params });
    return response.data?.data || [];
  },

  async downloadExport(type, params = {}) {
    const response = await apiClient.get(`/api/analytics/export/${type}`, {
      params,
      responseType: 'blob',
    });
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `${type}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },
};

export default costAnalyticsService;
