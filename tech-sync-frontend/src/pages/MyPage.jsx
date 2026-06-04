import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import * as usersApi from '../api/users';
import * as keywordsApi from '../api/keywords';
import { useAuth } from '../store/AuthContext';

export default function MyPage() {
  const { updateUser } = useAuth();

  const [me, setMe] = useState(null);
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  // 키워드 구독 상태
  const [recommended, setRecommended] = useState([]);
  const [myKeywordNames, setMyKeywordNames] = useState(new Set());
  const [busyKeyword, setBusyKeyword] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      usersApi.getMe(),
      keywordsApi.getRecommended(),
      keywordsApi.getMyKeywords(),
    ])
      .then(([profile, recs, mine]) => {
        if (cancelled) return;
        setMe(profile);
        setName(profile.name);
        setRecommended(recs);
        setMyKeywordNames(new Set(mine.map((k) => k.keywordName)));
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.response?.data?.message || '내 정보를 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const dirty = useMemo(
    () => me && name.trim() !== me.name && name.trim().length > 0,
    [me, name],
  );

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await usersApi.updateMe({ name: name.trim() });
      setMe(updated);
      setName(updated.name);
      updateUser({ name: updated.name }); // AppBar 인사말 즉시 반영
      setNotice('내 정보가 저장되었습니다.');
    } catch (err) {
      setError(err.response?.data?.message || '저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const toggleKeyword = async (keyword) => {
    const subscribed = myKeywordNames.has(keyword.keywordName);
    setBusyKeyword(keyword.id);
    setError(null);
    try {
      if (subscribed) {
        await keywordsApi.unsubscribe(keyword.id);
        setMyKeywordNames((prev) => {
          const next = new Set(prev);
          next.delete(keyword.keywordName);
          return next;
        });
      } else {
        await keywordsApi.subscribe(keyword.id);
        setMyKeywordNames((prev) => new Set(prev).add(keyword.keywordName));
      }
    } catch (err) {
      setError(err.response?.data?.message || '키워드 구독 변경에 실패했습니다.');
    } finally {
      setBusyKeyword(null);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 640 }}>
      <Typography variant="h4" sx={{ mb: 3, fontWeight: 700 }}>
        마이페이지
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {notice && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setNotice(null)}>
          {notice}
        </Alert>
      )}

      {/* 내 정보 */}
      <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 600 }}>
          내 정보
        </Typography>
        <Stack spacing={2}>
          <TextField
            label="이메일"
            value={me?.email ?? ''}
            disabled
            fullWidth
            helperText="이메일은 변경할 수 없습니다."
          />
          <TextField
            label="이름"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
            inputProps={{ maxLength: 50 }}
          />
          <Box>
            <Button
              variant="contained"
              onClick={handleSave}
              disabled={!dirty || saving}
            >
              {saving ? '저장 중…' : '저장'}
            </Button>
          </Box>
        </Stack>
      </Paper>

      {/* 구독 키워드 관리 */}
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Typography variant="h6" sx={{ mb: 1, fontWeight: 600 }}>
          구독 키워드
        </Typography>
        <Typography color="text.secondary" sx={{ mb: 2, fontSize: 14 }}>
          키워드를 선택하면 해당 키워드의 네이버 뉴스가 피드에 노출됩니다.
        </Typography>
        <Divider sx={{ mb: 2 }} />
        {recommended.length === 0 ? (
          <Alert severity="info">등록된 추천 키워드가 없습니다.</Alert>
        ) : (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
            {recommended.map((keyword) => {
              const subscribed = myKeywordNames.has(keyword.keywordName);
              return (
                <Chip
                  key={keyword.id}
                  label={keyword.keywordName}
                  color={subscribed ? 'primary' : 'default'}
                  variant={subscribed ? 'filled' : 'outlined'}
                  onClick={() => toggleKeyword(keyword)}
                  disabled={busyKeyword === keyword.id}
                />
              );
            })}
          </Box>
        )}
      </Paper>
    </Box>
  );
}
