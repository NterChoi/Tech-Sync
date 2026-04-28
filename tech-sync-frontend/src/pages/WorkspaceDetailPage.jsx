import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import EditNoteIcon from '@mui/icons-material/EditNote';
import * as wsApi from '../api/workspaces';
import { useAuth } from '../store/AuthContext';

export default function WorkspaceDetailPage() {
  const { id } = useParams();
  const wsId = Number(id);
  const navigate = useNavigate();
  const { user } = useAuth();

  const [ws, setWs] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [renameOpen, setRenameOpen] = useState(false);
  const [newName, setNewName] = useState('');
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('EDITOR');
  const [submitting, setSubmitting] = useState(false);

  const isOwner = useMemo(
    () => ws && user && ws.ownerId === user.userId,
    [ws, user],
  );

  const refresh = useCallback(() => {
    setLoading(true);
    wsApi
      .getDetail(wsId)
      .then(setWs)
      .catch((err) =>
        setError(err.response?.data?.message || '상세를 불러오지 못했습니다.'),
      )
      .finally(() => setLoading(false));
  }, [wsId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const handleRename = async () => {
    setSubmitting(true);
    try {
      await wsApi.update(wsId, { name: newName });
      setRenameOpen(false);
      setNewName('');
      refresh();
    } catch (err) {
      setError(err.response?.data?.message || '수정 실패');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    setSubmitting(true);
    try {
      await wsApi.remove(wsId);
      navigate('/workspaces', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || '삭제 실패');
      setSubmitting(false);
    }
  };

  const handleInvite = async () => {
    setSubmitting(true);
    try {
      await wsApi.inviteMember(wsId, {
        email: inviteEmail,
        role: inviteRole,
      });
      setInviteOpen(false);
      setInviteEmail('');
      setInviteRole('EDITOR');
      refresh();
    } catch (err) {
      setError(err.response?.data?.message || '초대 실패');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemoveMember = async (userId) => {
    try {
      await wsApi.removeMember(wsId, userId);
      if (userId === user?.userId && !isOwner) {
        navigate('/workspaces', { replace: true });
      } else {
        refresh();
      }
    } catch (err) {
      setError(err.response?.data?.message || '멤버 제거 실패');
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (!ws) {
    return (
      <Alert severity="error">
        {error || '워크스페이스를 찾을 수 없습니다.'}
      </Alert>
    );
  }

  return (
    <Box>
      <Button
        startIcon={<ArrowBackIcon />}
        onClick={() => navigate('/workspaces')}
        sx={{ mb: 2 }}
      >
        목록으로
      </Button>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        sx={{ mb: 1 }}
      >
        <Typography variant="h4" fontWeight={700}>
          {ws.workspaceName}
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button
            variant="contained"
            startIcon={<EditNoteIcon />}
            onClick={() => navigate(`/workspaces/${wsId}/edit`)}
          >
            편집기 열기
          </Button>
          {isOwner && (
            <>
              <Tooltip title="이름 수정">
                <IconButton
                  onClick={() => {
                    setNewName(ws.workspaceName);
                    setRenameOpen(true);
                  }}
                >
                  <EditIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="삭제">
                <IconButton color="error" onClick={() => setDeleteOpen(true)}>
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            </>
          )}
        </Stack>
      </Stack>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        소유자: {ws.ownerName} · 생성일{' '}
        {ws.createdAt
          ? new Date(ws.createdAt).toLocaleDateString('ko-KR')
          : ''}
      </Typography>

      <Divider sx={{ my: 2 }} />

      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        sx={{ mb: 1 }}
      >
        <Typography variant="h6">멤버 ({ws.members.length})</Typography>
        {isOwner && (
          <Button variant="outlined" onClick={() => setInviteOpen(true)}>
            멤버 초대
          </Button>
        )}
      </Stack>
      <List>
        {ws.members.map((m) => (
          <ListItem
            key={m.memberId}
            divider
            secondaryAction={
              <>
                {isOwner && m.role !== 'OWNER' && (
                  <IconButton
                    onClick={() => handleRemoveMember(m.userId)}
                    aria-label="멤버 제거"
                  >
                    <DeleteIcon />
                  </IconButton>
                )}
                {!isOwner && m.userId === user?.userId && (
                  <Button
                    color="error"
                    size="small"
                    onClick={() => handleRemoveMember(m.userId)}
                  >
                    탈퇴
                  </Button>
                )}
              </>
            }
          >
            <ListItemText
              primary={
                <Stack direction="row" spacing={1} alignItems="center">
                  <Typography>{m.userName}</Typography>
                  <Chip
                    label={m.role}
                    size="small"
                    color={m.role === 'OWNER' ? 'primary' : 'default'}
                  />
                </Stack>
              }
              secondary={m.email}
            />
          </ListItem>
        ))}
      </List>

      <Dialog
        open={renameOpen}
        onClose={() => setRenameOpen(false)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>이름 수정</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            fullWidth
            label="이름"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            inputProps={{ maxLength: 100 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setRenameOpen(false)}>취소</Button>
          <Button
            onClick={handleRename}
            variant="contained"
            disabled={!newName.trim() || submitting}
          >
            저장
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        maxWidth="xs"
      >
        <DialogTitle>워크스페이스 삭제</DialogTitle>
        <DialogContent>
          <Typography>
            정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteOpen(false)}>취소</Button>
          <Button
            onClick={handleDelete}
            color="error"
            variant="contained"
            disabled={submitting}
          >
            삭제
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={inviteOpen}
        onClose={() => setInviteOpen(false)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>멤버 초대</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              autoFocus
              label="이메일"
              type="email"
              fullWidth
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
            />
            <Select
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value)}
            >
              <MenuItem value="EDITOR">EDITOR</MenuItem>
              <MenuItem value="VIEWER">VIEWER</MenuItem>
            </Select>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInviteOpen(false)}>취소</Button>
          <Button
            onClick={handleInvite}
            variant="contained"
            disabled={!inviteEmail.trim() || submitting}
          >
            초대
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
