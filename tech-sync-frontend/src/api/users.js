import api from './axios';

export function getMe() {
  return api.get('/users/me').then((r) => r.data.data);
}

export function updateMe({ name }) {
  return api.put('/users/me', { name }).then((r) => r.data.data);
}
