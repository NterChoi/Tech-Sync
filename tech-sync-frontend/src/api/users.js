import api from './axios';

export function getMe() {
  return api.get('/users/me').then((r) => r.data.data);
}

export function updateMe({ name }) {
  return api.put('/users/me', { name }).then((r) => r.data.data);
}

export function changePassword({ currentPassword, newPassword }) {
  return api.put('/users/me/password', { currentPassword, newPassword });
}

export function deleteAccount() {
  return api.delete('/users/me');
}
