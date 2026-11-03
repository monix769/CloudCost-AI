import apiClient from './api';

export const billingService = {
  async getBillingRecords(params = {}) {
    const response = await apiClient.get('/api/billing', { params });
    return response.data?.data;
  },

  async getBillingRecordById(id) {
    const response = await apiClient.get(`/api/billing/${id}`);
    return response.data?.data;
  },

  async createBillingRecord(data) {
    const response = await apiClient.post('/api/billing', data);
    return response.data?.data;
  },

  async uploadBillingCsv(file) {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post('/api/billing/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data?.data;
  },

  async deleteBillingRecord(id) {
    const response = await apiClient.delete(`/api/billing/${id}`);
    return response.data;
  },
};

export default billingService;
