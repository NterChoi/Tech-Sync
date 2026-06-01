import api from './axios';

export function listMine() {
  return api.get('/alarm').then((r) => r.data.data);
}

export function getUnreadCount() {
  return api.get('/alarm/unread-count').then((r) => r.data.data);
}

export function markRead(alarmId) {
  return api.patch(`/alarm/${alarmId}/read`);
}
