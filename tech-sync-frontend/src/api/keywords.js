import api from './axios';

export function getRecommended() {
  return api.get('/keywords').then((r) => r.data.data);
}

export function getMyKeywords() {
  return api.get('/keywords/my').then((r) => r.data.data);
}

export function subscribe(keywordMasterId) {
  return api.post(`/keywords/${keywordMasterId}`);
}

export function unsubscribe(keywordMasterId) {
  return api.delete(`/keywords/${keywordMasterId}`);
}
