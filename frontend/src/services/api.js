import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Centralized Axios client configured with base URL, timeout,
 * and standard request/response interceptors for JWT Bearer authentication.
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Request Interceptor: Attach JWT Bearer Token from sessionStorage
apiClient.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('cloudcost_token') || localStorage.getItem('cloudcost_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Uniform error extraction and 401 auto-logout handling
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    const status = error.response?.status || 500;
    const requestUrl = error.config?.url || '';

    // Handle session expiration on protected endpoints
    if (status === 401 && !requestUrl.includes('/api/auth/login') && !requestUrl.includes('/api/auth/register')) {
      console.warn('Session expired or unauthorized. Clearing authentication tokens.');
      sessionStorage.removeItem('cloudcost_token');
      sessionStorage.removeItem('cloudcost_user');
      localStorage.removeItem('cloudcost_token');
      localStorage.removeItem('cloudcost_user');

      // Dispatch event so AuthContext can transition state and redirect to login
      window.dispatchEvent(new CustomEvent('cloudcost_auth_expired'));
    }

    const customError = {
      message: error.response?.data?.message || error.message || 'An unexpected network error occurred',
      status: status,
      data: error.response?.data || null,
      validationErrors: error.response?.data?.validationErrors || null,
      isNetworkError: !error.response,
    };
    return Promise.reject(customError);
  }
);

export default apiClient;
export { API_BASE_URL };
