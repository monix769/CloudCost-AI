import apiClient from './api';

/**
 * Authentication Service handling REST calls for user registration,
 * credential authentication, profile inspection, and stateless logout.
 */
export const authService = {
  /**
   * Register a new user and organization tenant
   * @param {Object} data { fullName, email, password, organizationName }
   */
  async register(data) {
    const response = await apiClient.post('/api/auth/register', data);
    return response.data; // Standard ApiResponse: { success, message, data: { token, tokenType, expiresInMs, user } }
  },

  /**
   * Authenticate user credentials and retrieve signed JWT
   * @param {Object} credentials { email, password }
   */
  async login(credentials) {
    const response = await apiClient.post('/api/auth/login', credentials);
    return response.data;
  },

  /**
   * Fetch current authenticated user's profile based on active Bearer token
   */
  async getProfile() {
    const response = await apiClient.get('/api/auth/me');
    return response.data;
  },

  /**
   * Revoke active JWT token via server-side denylist and clear session
   */
  async logout() {
    try {
      const response = await apiClient.post('/api/auth/logout');
      return response.data;
    } catch (error) {
      // Even if network or server call fails, frontend session is still cleared
      console.warn('Backend logout notification failed:', error);
      return { success: true };
    }
  },
};

export default authService;
