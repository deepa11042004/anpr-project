import axios from 'axios';

const API_BASE_URL = '/api';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      // Clear token if unauthorized
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      
      // Redirect to login if not already there
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  validate: () => api.get('/auth/validate'),
};

// Dashboard API
export const dashboardApi = {
  getStats: (period = 'today') => api.get(`/dashboard/stats?period=${period}`),
  getLiveFeed: () => api.get('/dashboard/live-feed'),
  poll: (since) => api.get(`/dashboard/poll?since=${since}`),
};

// Logs API
export const logsApi = {
  getLogs: (params) => api.get('/logs', { params }),
  getLogById: (id) => api.get(`/logs/${id}`),
};

// Gate API
export const gateApi = {
  override: (data) => api.post('/gate/override', data),
};

export const settingsApi = {
  testConnection: (data) => api.post('/admin/settings/database/test-connection', data),
  migrateData: (data) => api.post('/admin/settings/database/migrate', data),
  applyConfig: (data) => api.post('/admin/settings/database/apply-config', data),
  restartApp: () => api.post('/admin/settings/database/restart'),
  resetDefault: () => api.post('/admin/settings/database/reset-default'),
};

export const systemLogsApi = {
  getLogs: (params) => api.get('/admin/system-logs', { params }),
};

export const connectivityApi = {
  testCamera: (data) => api.post('/admin/connectivity/camera/test', data),
  testBoomGate: (data) => api.post('/admin/connectivity/boom-gate/test', data),
};

export default api;
