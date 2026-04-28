import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import * as wsApi from '../api/workspaces';

export default function WorkspacesPage() {
  const navigate = useNavigate();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [name, setName] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    wsApi
      .listMine()
      .then((res) => {
        if (!cancelled) setList(res);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.response?.data?.message || '목록을 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleCreate = async () => {
    setSubmitting(true);
    try {
      const created = await wsApi.create({ name });
      setDialogOpen(false);
      setName('');
      navigate(`/workspaces/${created.workspaceId}`);
    } catch (err) {
      setError(err.response?.data?.message || '생성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box>
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        sx={{ mb: 3 }}
      >
        <Typography variant="h4" fontWeight={700}>
          워크스페이스
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setDialogOpen(true)}
        >
          새 워크스페이스
        </Button>
      </Stack>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}
      {!loading && list.length === 0 && (
        <Alert severity="info">
          아직 워크스페이스가 없습니다. 새로 만들어보세요.
        </Alert>
      )}
      {!loading && list.length > 0 && (
        <Grid container spacing={2}>
          {list.map((ws) => (
            <Grid item xs={12} sm={6} md={4} key={ws.workspaceId}>
              <Card>
                <CardActionArea
                  onClick={() => navigate(`/workspaces/${ws.workspaceId}`)}
                >
                  <CardContent>
                    <Typography variant="h6" fontWeight={600}>
                      {ws.workspaceName}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      소유자: {ws.ownerName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {ws.createdAt
                        ? new Date(ws.createdAt).toLocaleDateString('ko-KR')
                        : ''}
                    </Typography>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        fullWidth
        maxWidth="xs"
      >
        <DialogTitle>새 워크스페이스</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="이름"
            fullWidth
            value={name}
            onChange={(e) => setName(e.target.value)}
            inputProps={{ maxLength: 100 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>취소</Button>
          <Button
            onClick={handleCreate}
            variant="contained"
            disabled={!name.trim() || submitting}
          >
            만들기
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
