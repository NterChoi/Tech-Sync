import { useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
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

export default function SignupPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await authApi.signup({ email, password, name });
      const tokens = await authApi.login({ email, password });
      signIn(tokens);
      navigate('/feed', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || '회원가입에 실패했습니다.');
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
              <Typography color="text.secondary">회원가입</Typography>
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
              label="비밀번호 (8자 이상)"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              fullWidth
              inputProps={{ minLength: 8 }}
            />
            <TextField
              label="이름"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
              fullWidth
              inputProps={{ maxLength: 50 }}
            />
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={submitting}
              fullWidth
            >
              {submitting ? '회원가입 중...' : '회원가입'}
            </Button>
            <Typography variant="body2" textAlign="center" color="text.secondary">
              이미 계정이 있으신가요?{' '}
              <MuiLink component={RouterLink} to="/login">
                로그인
              </MuiLink>
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
