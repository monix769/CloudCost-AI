import apiClient from './api';

export const notificationService = {
  async getNotifications(params = {}) {
    const response = await apiClient.get('/api/notifications', { params });
    return response.data?.data || [];
  },

  async getUnreadCount() {
    const response = await apiClient.get('/api/notifications/unread-count');
    return response.data?.data?.unreadCount || 0;
  },

  async markAsRead(id) {
    const response = await apiClient.patch(`/api/notifications/${id}/read`);
    return response.data?.data;
  },

  async markAllAsRead() {
    const response = await apiClient.post('/api/notifications/read-all');
    return response.data?.data;
  },
};

export default notificationService;

