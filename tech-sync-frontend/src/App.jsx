import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, Typography } from '@mui/material';

function Placeholder({ name }) {
  return (
    <Box sx={{ p: 4 }}>
      <Typography variant="h4">{name}</Typography>
      <Typography color="text.secondary">페이지 준비 중</Typography>
    </Box>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="/login" element={<Placeholder name="로그인" />} />
      <Route path="*" element={<Placeholder name="404" />} />
    </Routes>
  );
}
