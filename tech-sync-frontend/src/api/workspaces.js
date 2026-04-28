import api from './axios';

export function listMine() {
  return api.get('/workspaces').then((r) => r.data.data);
}

export function getDetail(id) {
  return api.get(`/workspaces/${id}`).then((r) => r.data.data);
}

export function create({ name }) {
  return api.post('/workspaces', { name }).then((r) => r.data.data);
}

export function update(id, { name }) {
  return api.put(`/workspaces/${id}`, { name }).then((r) => r.data.data);
}

export function remove(id) {
  return api.delete(`/workspaces/${id}`);
}

export function inviteMember(id, { email, role }) {
  return api
    .post(`/workspaces/${id}/members`, { email, role })
    .then((r) => r.data.data);
}

export function removeMember(id, userId) {
  return api.delete(`/workspaces/${id}/members/${userId}`);
}
