import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Divider,
  Pagination,
  Stack,
  Typography,
} from '@mui/material';
import * as feedApi from '../api/feed';
import * as keywordsApi from '../api/keywords';
import ArticleCard from '../components/ArticleCard';

// 필터 선택값 → API 파라미터 변환
function toParams(selected) {
  if (selected === 'GEEK') return { source: 'GEEK' };
  if (selected === 'NAVER') return { source: 'NAVER' };
  if (selected.startsWith('kw:')) return { keyword: selected.slice(3) };
  return {}; // ALL
}

export default function FeedPage() {
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState('ALL');
  const [myKeywords, setMyKeywords] = useState([]);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 구독 키워드 목록 (필터 칩용) — 최초 1회
  useEffect(() => {
    keywordsApi.getMyKeywords().then(setMyKeywords).catch(() => {});
  }, []);

  const params = useMemo(() => toParams(selected), [selected]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    feedApi
      .getFeed({ page, ...params })
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
  }, [page, params]);

  const handleSelect = (value) => {
    setSelected(value);
    setPage(0);
  };

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

      {/* 필터 바 */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 1, mb: 3 }}>
        <Chip
          label="전체"
          color={selected === 'ALL' ? 'primary' : 'default'}
          variant={selected === 'ALL' ? 'filled' : 'outlined'}
          onClick={() => handleSelect('ALL')}
        />
        <Chip
          label="GEEK"
          color={selected === 'GEEK' ? 'primary' : 'default'}
          variant={selected === 'GEEK' ? 'filled' : 'outlined'}
          onClick={() => handleSelect('GEEK')}
        />
        <Chip
          label="Naver"
          color={selected === 'NAVER' ? 'primary' : 'default'}
          variant={selected === 'NAVER' ? 'filled' : 'outlined'}
          onClick={() => handleSelect('NAVER')}
        />
        {myKeywords.length > 0 && (
          <>
            <Divider orientation="vertical" flexItem sx={{ mx: 0.5 }} />
            {myKeywords.map((k) => {
              const value = `kw:${k.keywordName}`;
              return (
                <Chip
                  key={k.id ?? k.keywordName}
                  label={k.keywordName}
                  size="small"
                  color={selected === value ? 'secondary' : 'default'}
                  variant={selected === value ? 'filled' : 'outlined'}
                  onClick={() => handleSelect(value)}
                />
              );
            })}
          </>
        )}
      </Box>

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
          표시할 뉴스가 없습니다.
          {selected === 'ALL'
            ? ' 키워드를 구독해보세요.'
            : ' 다른 필터를 선택해보세요.'}
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
