import { useCallback, useEffect, useState } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Badge,
  Box,
  Button,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Menu,
  MenuItem,
  Toolbar,
  Typography,
} from '@mui/material';
import NotificationsIcon from '@mui/icons-material/Notifications';
import { useAuth } from '../store/AuthContext';
import * as authApi from '../api/auth';
import * as alarmApi from '../api/alarm';
import useSSE from '../hooks/useSSE';

const DRAWER_WIDTH = 220;

const navItems = [
  { to: '/feed', label: '뉴스 피드' },
  { to: '/scraps', label: '내 스크랩' },
  { to: '/workspaces', label: '워크스페이스' },
];

export default function Layout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();

  const [alarms, setAlarms] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [anchorEl, setAnchorEl] = useState(null);

  const loadAlarms = useCallback(async () => {
    try {
      const [list, count] = await Promise.all([
        alarmApi.listMine(),
        alarmApi.getUnreadCount(),
      ]);
      setAlarms(list);
      setUnreadCount(count);
    } catch {
      // 알림 로드 실패는 조용히 무시 (핵심 기능 아님)
    }
  }, []);

  useEffect(() => {
    if (user) loadAlarms();
  }, [user, loadAlarms]);

  // SSE 실시간 수신 — 새 알림 도착 시 목록 앞에 추가 + 배지 증가
  const handleIncoming = useCallback((alarm) => {
    setAlarms((prev) => [alarm, ...prev]);
    setUnreadCount((prev) => prev + 1);
  }, []);
  useSSE(handleIncoming, !!user);

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

  const handleAlarmClick = async (alarm) => {
    setAnchorEl(null);
    if (!alarm.read) {
      try {
        await alarmApi.markRead(alarm.id);
        setAlarms((prev) =>
          prev.map((a) => (a.id === alarm.id ? { ...a, read: true } : a)));
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch {
        // 읽음 처리 실패 무시
      }
    }
    if (alarm.workspaceId) {
      navigate(`/workspaces/${alarm.workspaceId}`);
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
              <IconButton
                color="inherit"
                sx={{ mr: 1 }}
                onClick={(e) => setAnchorEl(e.currentTarget)}
              >
                <Badge badgeContent={unreadCount} color="error">
                  <NotificationsIcon />
                </Badge>
              </IconButton>
              <Menu
                anchorEl={anchorEl}
                open={Boolean(anchorEl)}
                onClose={() => setAnchorEl(null)}
                slotProps={{ paper: { sx: { width: 320, maxHeight: 400 } } }}
              >
                <Typography sx={{ px: 2, py: 1, fontWeight: 600 }}>알림</Typography>
                <Divider />
                {alarms.length === 0 && (
                  <MenuItem disabled>새 알림이 없습니다.</MenuItem>
                )}
                {alarms.map((alarm) => (
                  <MenuItem
                    key={alarm.id}
                    onClick={() => handleAlarmClick(alarm)}
                    sx={{
                      whiteSpace: 'normal',
                      bgcolor: alarm.read ? 'transparent' : 'action.hover',
                    }}
                  >
                    <ListItemText
                      primary={alarm.message}
                      secondary={new Date(alarm.createdAt).toLocaleString()}
                      primaryTypographyProps={{
                        fontWeight: alarm.read ? 400 : 600,
                        fontSize: 14,
                      }}
                    />
                  </MenuItem>
                ))}
              </Menu>
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
