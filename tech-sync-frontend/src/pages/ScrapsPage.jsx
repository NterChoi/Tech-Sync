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

export default function ScrapsPage() {
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    feedApi
      .getScraps({ page })
      .then((res) => {
        if (!cancelled) setData(res);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.response?.data?.message || '스크랩을 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [page]);

  const handleUnscrap = async (article) => {
    try {
      await feedApi.unscrap(article.id);
      setData((prev) => ({
        ...prev,
        content: prev.content.filter((a) => a.id !== article.id),
        totalElements: Math.max(0, (prev.totalElements ?? 0) - 1),
      }));
    } catch (err) {
      setError(err.response?.data?.message || '스크랩 해제에 실패했습니다.');
    }
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
        내 스크랩
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
          스크랩한 기사가 없습니다. 피드에서 북마크 아이콘을 눌러 스크랩해보세요.
        </Alert>
      )}
      {!loading && data && data.content.length > 0 && (
        <>
          <Stack spacing={2}>
            {data.content.map((article) => (
              <ArticleCard
                key={article.id}
                article={article}
                onToggleScrap={handleUnscrap}
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
