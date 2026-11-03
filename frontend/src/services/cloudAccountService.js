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
};

export default cloudAccountService;
