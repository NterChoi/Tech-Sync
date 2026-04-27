import axios from 'axios';

const ACCESS_TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';

export const tokenStore = {
  getAccess: () => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_TOKEN_KEY),
  set: ({ accessToken, refreshToken }) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = tokenStore.getAccess();
  if (token && !config.skipAuth) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing = null;

function clearAndRedirect() {
  tokenStore.clear();
  localStorage.removeItem('user_info');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const status = error.response?.status;

    if (status === 401 && original && !original._retry) {
      const refreshToken = tokenStore.getRefresh();
      if (!refreshToken) {
        clearAndRedirect();
        return Promise.reject(error);
      }
      original._retry = true;
      try {
        if (!refreshing) {
          refreshing = axios
            .post('/api/auth/refresh', null, {
              headers: { 'Refresh-Token': refreshToken },
            })
            .then((r) => r.data.data)
            .finally(() => {
              refreshing = null;
            });
        }
        const tokens = await refreshing;
        tokenStore.set(tokens);
        original.headers.Authorization = `Bearer ${tokens.accessToken}`;
        return api(original);
      } catch (e) {
        clearAndRedirect();
        return Promise.reject(e);
      }
    }

    return Promise.reject(error);
  },
);

export default api;
