import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import Quill from 'quill';
import 'quill/dist/quill.snow.css';
import { useAuth } from '../store/AuthContext';
import * as wsApi from '../api/workspaces';
import useEditorSocket from '../hooks/useEditorSocket';

export default function WorkspaceEditorPage() {
  const { id } = useParams();
  const wsId = Number(id);
  const navigate = useNavigate();
  const { user } = useAuth();

  const editorContainerRef = useRef(null);
  const quillRef = useRef(null);

  const [ws, setWs] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [seqNo, setSeqNo] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    wsApi
      .getDetail(wsId)
      .then((res) => {
        if (!cancelled) setWs(res);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(
            err.response?.data?.message ||
              '워크스페이스 정보를 불러오지 못했습니다.',
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [wsId]);

  useEffect(() => {
    if (!ws) return undefined;
    const container = editorContainerRef.current;
    if (!container || quillRef.current) return undefined;
    const quill = new Quill(container, {
      theme: 'snow',
      placeholder: '함께 편집해보세요...',
      modules: {
        toolbar: [
          [{ header: [1, 2, 3, false] }],
          ['bold', 'italic', 'underline'],
          [{ list: 'ordered' }, { list: 'bullet' }],
          ['link', 'code-block'],
          ['clean'],
        ],
      },
    });
    quillRef.current = quill;
    return () => {
      // StrictMode 두 번 마운트 / 언마운트 대비: toolbar+container 모두 제거
      const wrapper = container.parentElement;
      if (wrapper) wrapper.innerHTML = '';
      quillRef.current = null;
    };
  }, [ws]);

  const handleRemoteDelta = useCallback((broadcast) => {
    if (!quillRef.current) return;
    quillRef.current.updateContents({ ops: broadcast.ops }, 'silent');
    setSeqNo(broadcast.seqNo);
  }, []);

  const { connected, error: socketError, sendDelta } = useEditorSocket({
    workspaceId: wsId,
    currentUserId: user?.userId,
    onRemoteDelta: handleRemoteDelta,
  });

  useEffect(() => {
    const quill = quillRef.current;
    if (!quill) return undefined;
    const handler = (delta, _oldDelta, source) => {
      if (source !== 'user') return;
      sendDelta(delta.ops, seqNo);
    };
    quill.on('text-change', handler);
    return () => quill.off('text-change', handler);
  }, [sendDelta, seqNo, ws]);

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
        onClick={() => navigate(`/workspaces/${wsId}`)}
        sx={{ mb: 2 }}
      >
        워크스페이스로
      </Button>
      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="center"
        sx={{ mb: 2 }}
      >
        <Typography variant="h4" fontWeight={700}>
          {ws.workspaceName}
        </Typography>
        <Chip
          label={connected ? '연결됨' : '연결 중...'}
          color={connected ? 'success' : 'default'}
          size="small"
        />
      </Stack>
      {socketError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {socketError}
        </Alert>
      )}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      <Box
        sx={{
          backgroundColor: 'background.paper',
          '& .ql-container': { minHeight: 400, fontSize: 16 },
        }}
      >
        <Box ref={editorContainerRef} />
      </Box>
    </Box>
  );
}
