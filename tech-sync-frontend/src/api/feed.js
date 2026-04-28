import api from './axios';

export function getFeed({ page = 0, size = 20 } = {}) {
  return api
    .get('/feed', { params: { page, size, sort: 'publishedAt,desc' } })
    .then((r) => r.data.data);
}

export function getScraps({ page = 0, size = 20 } = {}) {
  return api
    .get('/feed/scraps', { params: { page, size, sort: 'publishedAt,desc' } })
    .then((r) => r.data.data);
}

export function scrap(articleId) {
  return api.post(`/feed/${articleId}/scrap`);
}

export function unscrap(articleId) {
  return api.delete(`/feed/${articleId}/scrap`);
}
