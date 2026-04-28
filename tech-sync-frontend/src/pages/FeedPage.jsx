import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  CircularProgress,
  Pagination,
  Stack,
  Typography,
} from '@mui/material';
import * as feedApi from '../api/feed';
import ArticleCard from '../components/ArticleCard';

export default function FeedPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    feedApi
      .getFeed({ page })
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.response?.data?.message || '피드를 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page]);

  const handleToggleScrap = async (article) => {
    try {
      if (article.isScraped) {
        await feedApi.unscrap(article.id);
      } else {
        await feedApi.scrap(article.id);
      }
      setData((prev) => ({
        ...prev,
        content: prev.content.map((a) =>
          a.id === article.id ? { ...a, isScraped: !a.isScraped } : a,
        ),
      }));
    } catch (err) {
      setError(err.response?.data?.message || '스크랩 처리에 실패했습니다.');
    }
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
        뉴스 피드
      </Typography>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}
      {!loading && data && data.content.length === 0 && (
        <Alert severity="info">
          표시할 뉴스가 없습니다. 키워드를 구독해보세요.
        </Alert>
      )}
      {!loading && data && data.content.length > 0 && (
        <>
          <Stack spacing={2}>
            {data.content.map((article) => (
              <ArticleCard
                key={article.id}
                article={article}
                onToggleScrap={handleToggleScrap}
              />
            ))}
          </Stack>
          {data.totalPages > 1 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
              <Pagination
                count={data.totalPages}
                page={page + 1}
                onChange={(_, p) => setPage(p - 1)}
                color="primary"
              />
            </Box>
          )}
        </>
      )}
    </Box>
  );
}
