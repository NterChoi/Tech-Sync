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
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import * as authApi from '../api/auth';

function FindIdForm() {
  const [name, setName] = useState('');
  const [emails, setEmails] = useState(null);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setEmails(null);
    setSubmitting(true);
    try {
      const result = await authApi.findId({ name });
      setEmails(result);
    } catch (err) {
      setError(err.response?.data?.message || '조회에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Stack spacing={2} component="form" onSubmit={handleSubmit}>
      <Typography variant="body2" color="text.secondary">
        가입 시 입력한 이름으로 이메일(아이디)을 찾습니다.
      </Typography>
      {error && <Alert severity="error">{error}</Alert>}
      <TextField
        label="이름"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
        fullWidth
        autoFocus
      />
      <Button type="submit" variant="contained" disabled={submitting} fullWidth>
        {submitting ? '조회 중...' : '아이디 찾기'}
      </Button>
      {emails && emails.length > 0 && (
        <Alert severity="success">
          가입된 이메일: {emails.join(', ')}
        </Alert>
      )}
      {emails && emails.length === 0 && (
        <Alert severity="info">일치하는 계정이 없습니다.</Alert>
      )}
    </Stack>
  );
}

function ResetPasswordForm() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState(null);
  const [done, setDone] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await authApi.resetPassword({ email, name, newPassword });
      setDone(true);
    } catch (err) {
      setError(err.response?.data?.message || '비밀번호 재설정에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  if (done) {
    return (
      <Stack spacing={2}>
        <Alert severity="success">
          비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.
        </Alert>
        <Button variant="contained" fullWidth onClick={() => navigate('/login', { replace: true })}>
          로그인하러 가기
        </Button>
      </Stack>
    );
  }

  return (
    <Stack spacing={2} component="form" onSubmit={handleSubmit}>
      <Typography variant="body2" color="text.secondary">
        가입한 이메일과 이름으로 본인 확인 후 새 비밀번호를 설정합니다.
      </Typography>
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
        label="이름"
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
        fullWidth
      />
      <TextField
        label="새 비밀번호 (8자 이상)"
        type="password"
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        required
        fullWidth
        inputProps={{ minLength: 8 }}
      />
      <Button type="submit" variant="contained" disabled={submitting} fullWidth>
        {submitting ? '변경 중...' : '비밀번호 재설정'}
      </Button>
    </Stack>
  );
}

export default function FindAccountPage() {
  const [tab, setTab] = useState(0);

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
      <Card sx={{ width: '100%', maxWidth: 420 }}>
        <CardContent sx={{ p: 4 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h5" fontWeight={700}>
                Tech-Sync
              </Typography>
              <Typography color="text.secondary">계정 찾기</Typography>
            </Box>
            <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="fullWidth">
              <Tab label="아이디 찾기" />
              <Tab label="비밀번호 찾기" />
            </Tabs>
            {tab === 0 ? <FindIdForm /> : <ResetPasswordForm />}
            <Typography variant="body2" textAlign="center" color="text.secondary">
              <MuiLink component={RouterLink} to="/login">
                로그인으로 돌아가기
              </MuiLink>
            </Typography>
          </Stack>
        </CardContent>
      </Card>
    </Box>
  );
}
