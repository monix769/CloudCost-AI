import apiClient from './api';

export const cloudAccountService = {
  async getAccounts(provider) {
    const params = provider ? { provider } : {};
    const response = await apiClient.get('/api/cloud-accounts', { params });
    return response.data?.data || [];
  },

  async getAccountById(id) {
    const response = await apiClient.get(`/api/cloud-accounts/${id}`);
    return response.data?.data;
  },

  async createAccount(data) {
    const response = await apiClient.post('/api/cloud-accounts', data);
    return response.data?.data;
  },

  async updateAccount(id, data) {
    const response = await apiClient.put(`/api/cloud-accounts/${id}`, data);
    return response.data?.data;
  },

  async deleteAccount(id) {
    const response = await apiClient.delete(`/api/cloud-accounts/${id}`);
    return response.data;
  },

  // Milestone 5 Multi-Cloud Integration methods
  async connectAccount(id, credentials) {
    const response = await apiClient.post(`/api/cloud-accounts/${id}/connect`, credentials);
    return response.data?.data;
  },

  async validateAccount(id) {
    const response = await apiClient.post(`/api/cloud-accounts/${id}/validate`);
    return response.data?.data;
  },

  async disconnectAccount(id) {
    const response = await apiClient.post(`/api/cloud-accounts/${id}/disconnect`);
    return response.data?.data;
  },

  async getConnectionStatus(id) {
    const response = await apiClient.get(`/api/cloud-accounts/${id}/connection-status`);
    return response.data?.data;
  },

  async getProviders() {
    const response = await apiClient.get('/api/cloud-providers');
    return response.data?.data || [];
  },

  async getProviderCapabilities(provider) {
    const response = await apiClient.get(`/api/cloud-providers/${provider}/capabilities`);
    return response.data?.data;
  },

  async testProviderConnection(provider, credentials) {
    const response = await apiClient.post(`/api/cloud-providers/${provider}/test-connection`, credentials);
    return response.data?.data;
  },
};

export default cloudAccountService;
