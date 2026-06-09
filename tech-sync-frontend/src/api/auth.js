import api, { tokenStore } from './axios';

export function signup({ email, password, name }) {
  return api.post('/auth/signup', { email, password, name }, { skipAuth: true });
}

export function login({ email, password }) {
  return api
    .post('/auth/login', { email, password }, { skipAuth: true })
    .then((r) => r.data.data);
}

export function logout() {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return Promise.resolve();
  return api.post('/auth/logout', null, {
    headers: { 'Refresh-Token': refreshToken },
  });
}

export function findId({ name }) {
  return api
    .post('/auth/find-id', { name }, { skipAuth: true })
    .then((r) => r.data.data.emails);
}

export function resetPassword({ email, name, newPassword }) {
  return api.post('/auth/reset-password', { email, name, newPassword }, { skipAuth: true });
}
