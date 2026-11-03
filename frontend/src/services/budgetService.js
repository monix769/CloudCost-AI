import apiClient from './api';

export const budgetService = {
  async getBudgets() {
    const response = await apiClient.get('/api/budgets');
    return response.data?.data || [];
  },

  async getBudgetById(id) {
    const response = await apiClient.get(`/api/budgets/${id}`);
    return response.data?.data;
  },

  async createBudget(data) {
    const response = await apiClient.post('/api/budgets', data);
    return response.data?.data;
  },

  async updateBudget(id, data) {
    const response = await apiClient.put(`/api/budgets/${id}`, data);
    return response.data?.data;
  },

  async deleteBudget(id) {
    const response = await apiClient.delete(`/api/budgets/${id}`);
    return response.data;
  },
};

export default budgetService;
