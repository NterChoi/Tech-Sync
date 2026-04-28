import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, Typography } from '@mui/material';
import { AuthProvider } from './store/AuthContext';
import ProtectedRoute from './routes/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import FeedPage from './pages/FeedPage';
import ScrapsPage from './pages/ScrapsPage';

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
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<Navigate to="/feed" replace />} />
          <Route path="/feed" element={<FeedPage />} />
          <Route path="/scraps" element={<ScrapsPage />} />
          <Route path="/workspaces" element={<Placeholder name="워크스페이스" />} />
          <Route path="/workspaces/:id" element={<Placeholder name="워크스페이스 상세" />} />
        </Route>
        <Route path="*" element={<Placeholder name="404" />} />
      </Routes>
    </AuthProvider>
  );
}
