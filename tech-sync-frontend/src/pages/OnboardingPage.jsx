import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import * as keywordsApi from '../api/keywords';
import { useAuth } from '../store/AuthContext';

export default function OnboardingPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [recommended, setRecommended] = useState([]);
  const [subscribed, setSubscribed] = useState(new Set()); // keywordName 기준
  const [loading, setLoading] = useState(true);
  const [busyKeyword, setBusyKeyword] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([keywordsApi.getRecommended(), keywordsApi.getMyKeywords()])
      .then(([recs, mine]) => {
        if (cancelled) return;
        setRecommended(recs);
        setSubscribed(new Set(mine.map((k) => k.keywordName)));
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.response?.data?.message || '키워드를 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const toggleKeyword = async (keyword) => {
    const isOn = subscribed.has(keyword.keywordName);
    setBusyKeyword(keyword.id);
    setError(null);
    try {
      if (isOn) {
        await keywordsApi.unsubscribe(keyword.id);
        setSubscribed((prev) => {
          const next = new Set(prev);
          next.delete(keyword.keywordName);
          return next;
        });
      } else {
        await keywordsApi.subscribe(keyword.id);
        setSubscribed((prev) => new Set(prev).add(keyword.keywordName));
      }
    } catch (err) {
      setError(err.response?.data?.message || '키워드 선택에 실패했습니다.');
    } finally {
      setBusyKeyword(null);
    }
  };

  const goFeed = () => navigate('/feed', { replace: true });

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        p: 2,
      }}
    >
      <Card sx={{ width: '100%', maxWidth: 560 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h5" fontWeight={700}>
                {user?.name ? `${user.name}님, 환영합니다 👋` : '환영합니다 👋'}
              </Typography>
              <Typography color="text.secondary">
                관심 있는 기술 키워드를 골라주세요. 선택한 키워드의 네이버 뉴스가 피드에 표시됩니다.
              </Typography>
            </Box>

            {error && (
              <Alert severity="error" onClose={() => setError(null)}>
                {error}
              </Alert>
            )}

            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {recommended.map((keyword) => {
                  const on = subscribed.has(keyword.keywordName);
                  return (
                    <Chip
                      key={keyword.id}
                      label={keyword.keywordName}
                      color={on ? 'primary' : 'default'}
                      variant={on ? 'filled' : 'outlined'}
                      onClick={() => toggleKeyword(keyword)}
                      disabled={busyKeyword === keyword.id}
                    />
                  );
                })}
              </Box>
            )}

            <Stack direction="row" spacing={1} justifyContent="flex-end" alignItems="center">
              <Button color="inherit" onClick={goFeed}>
                건너뛰기
              </Button>
              <Button
                variant="contained"
                size="large"
                onClick={goFeed}
                disabled={subscribed.size === 0}
              >
                시작하기 ({subscribed.size})
              </Button>
            </Stack>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
