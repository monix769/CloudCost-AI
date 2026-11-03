import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Centralized Axios client configured with base URL, timeout,
 * and standard request/response interceptors.
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  },
});

// Request Interceptor
apiClient.interceptors.request.use(
  (config) => {
    // Architectural foundation prepared for JWT injection in Milestone 2
    const token = localStorage.getItem('cloudcost_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    const customError = {
      message: error.response?.data?.message || error.message || 'An unexpected network error occurred',
      status: error.response?.status || 500,
      data: error.response?.data || null,
      isNetworkError: !error.response,
    };
    return Promise.reject(customError);
  }
);

export default apiClient;
export { API_BASE_URL };
