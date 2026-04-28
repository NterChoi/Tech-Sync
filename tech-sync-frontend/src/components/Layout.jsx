import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Box,
  Button,
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material';
import { useAuth } from '../store/AuthContext';
import * as authApi from '../api/auth';

const DRAWER_WIDTH = 220;

const navItems = [
  { to: '/feed', label: '뉴스 피드' },
  { to: '/scraps', label: '내 스크랩' },
  { to: '/workspaces', label: '워크스페이스' },
];

export default function Layout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch {
      // 서버 로그아웃 실패해도 클라이언트는 토큰 제거
    } finally {
      signOut();
      navigate('/login', { replace: true });
    }
  };

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1, fontWeight: 600 }}>
            Tech-Sync
          </Typography>
          {user && (
            <>
              <Typography sx={{ mr: 2 }}>{user.name}</Typography>
              <Button color="inherit" onClick={handleLogout}>
                로그아웃
              </Button>
            </>
          )}
        </Toolbar>
      </AppBar>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Toolbar />
        <List>
          {navItems.map((item) => (
            <ListItem key={item.to} disablePadding>
              <ListItemButton
                component={NavLink}
                to={item.to}
                sx={{
                  '&.active': {
                    backgroundColor: 'action.selected',
                    fontWeight: 600,
                  },
                }}
              >
                <ListItemText primary={item.label} />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      </Drawer>
      <Box component="main" sx={{ flexGrow: 1, p: 3, mt: 8 }}>
        <Outlet />
      </Box>
    </Box>
  );
}
