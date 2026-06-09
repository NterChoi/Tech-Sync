import { useState } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Link as MuiLink,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useAuth } from '../store/AuthContext';
import * as authApi from '../api/auth';
import * as keywordsApi from '../api/keywords';

export default function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/feed';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const tokens = await authApi.login({ email, password });
      signIn(tokens);
      // 구독 키워드가 하나도 없으면(신규 가입) 온보딩으로, 아니면 원래 목적지로.
      // 키워드 조회 실패는 로그인을 막지 않고 기본 목적지로 폴백한다.
      let dest = from;
      try {
        const myKeywords = await keywordsApi.getMyKeywords();
        if (myKeywords.length === 0) dest = '/onboarding';
      } catch {
        // 무시: 기본 목적지로 진행
      }
      navigate(dest, { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || '로그인에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

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
      <Card sx={{ width: '100%', maxWidth: 400 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={3} component="form" onSubmit={handleSubmit}>
            <Box>
              <Typography variant="h5" fontWeight={700}>
                Tech-Sync
              </Typography>
              <Typography color="text.secondary">로그인</Typography>
            </Box>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="이메일"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              fullWidth
              autoFocus
            />
            <TextField
              label="비밀번호"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              fullWidth
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={submitting}
              fullWidth
            >
              {submitting ? '로그인 중...' : '로그인'}
            </Button>
            <Typography variant="body2" textAlign="center" color="text.secondary">
              계정이 없으신가요?{' '}
              <MuiLink component={RouterLink} to="/signup">
                회원가입
              </MuiLink>
            </Typography>
            <Typography variant="body2" textAlign="center" color="text.secondary">
              <MuiLink component={RouterLink} to="/find-account">
                아이디 / 비밀번호 찾기
              </MuiLink>
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
