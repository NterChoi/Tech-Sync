import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
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
import QuillCursors from 'quill-cursors';
import 'quill/dist/quill.snow.css';
import { useAuth } from '../store/AuthContext';
import * as wsApi from '../api/workspaces';
import useEditorSocket from '../hooks/useEditorSocket';

const Delta = Quill.import('delta');
Quill.register('modules/cursors', QuillCursors);

function userColor(userId) {
  const hue = (Number(userId) * 137) % 360;
  return `hsl(${hue}, 70%, 45%)`;
}

const CURSOR_THROTTLE_MS = 80;

export default function WorkspaceEditorPage() {
  const { id } = useParams();
  const wsId = Number(id);
  const navigate = useNavigate();
  const { user } = useAuth();

  const editorContainerRef = useRef(null);
  const quillRef = useRef(null);
  const lastSyncedRef = useRef(null);

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

  const memberNameById = useMemo(() => {
    const map = new Map();
    if (ws?.ownerId && ws?.ownerName) map.set(ws.ownerId, ws.ownerName);
    ws?.members?.forEach((m) => {
      if (m.userId && m.userName) map.set(m.userId, m.userName);
    });
    return map;
  }, [ws]);

  useEffect(() => {
    if (!ws) return undefined;
    const container = editorContainerRef.current;
    if (!container || quillRef.current) return undefined;
    const quill = new Quill(container, {
      theme: 'snow',
      placeholder: '함께 편집해보세요...',
      modules: {
        cursors: true,
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
    lastSyncedRef.current = quill.getContents();
    return () => {
      const wrapper = container.parentElement;
      if (wrapper) wrapper.innerHTML = '';
      quillRef.current = null;
      lastSyncedRef.current = null;
    };
  }, [ws]);

  const handleRemoteDelta = useCallback((broadcast) => {
    if (!quillRef.current) return;
    quillRef.current.updateContents({ ops: broadcast.ops }, 'silent');
    lastSyncedRef.current = quillRef.current.getContents();
    setSeqNo(broadcast.seqNo);
  }, []);

  const handleRemoteCursor = useCallback(
    (broadcast) => {
      const quill = quillRef.current;
      if (!quill) return;
      const cursors = quill.getModule('cursors');
      if (!cursors) return;
      const cursorId = String(broadcast.userId);
      if (!broadcast.range) {
        cursors.removeCursor(cursorId);
        return;
      }
      const name = memberNameById.get(broadcast.userId) ?? `사용자${broadcast.userId}`;
      cursors.createCursor(cursorId, name, userColor(broadcast.userId));
      cursors.moveCursor(cursorId, broadcast.range);
    },
    [memberNameById],
  );

  const { connected, error: socketError, sendDelta, sendCursor } = useEditorSocket({
    workspaceId: wsId,
    currentUserId: user?.userId,
    onRemoteDelta: handleRemoteDelta,
    onRemoteCursor: handleRemoteCursor,
  });

  useEffect(() => {
    const quill = quillRef.current;
    if (!quill) return undefined;

    // composition 외부: Quill contents 정상 사용 (attributes 보존)
    const sendPendingDiff = () => {
      if (!quillRef.current || !lastSyncedRef.current) return;
      const current = quillRef.current.getContents();
      const diff = lastSyncedRef.current.diff(current);
      if (diff.ops.length > 0) {
        sendDelta(diff.ops, seqNo);
        lastSyncedRef.current = current;
      }
    };
    const handler = (_delta, _oldDelta, source) => {
      if (source !== 'user') return;
      sendPendingDiff();
    };

    // composition 중: Quill 2가 mutation 처리를 차단하므로 DOM에서 직접 추출.
    // 구글 독스 식 접근 — composition 중인 임시 글자("아", "안", "안ㄴ" 등)도
    // 즉시 다른 사용자에게 보이도록 50ms 폴링으로 동기화한다.
    const syncFromDom = () => {
      if (!quillRef.current || !lastSyncedRef.current) return;
      const text = quillRef.current.root.innerText;
      const normalized = text.endsWith('\n') ? text : `${text}\n`;
      const newContents = new Delta().insert(normalized);
      const diff = lastSyncedRef.current.diff(newContents);
      if (diff.ops.length > 0) {
        sendDelta(diff.ops, seqNo);
        lastSyncedRef.current = newContents;
      }
    };

    let pollTimer = null;
    const handleCompositionStart = () => {
      if (pollTimer) clearInterval(pollTimer);
      pollTimer = setInterval(syncFromDom, 50);
    };
    const handleCompositionEnd = () => {
      if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
      }
      // Quill이 commit을 contents에 반영할 시간을 준 뒤 정식 sync (attributes 복원)
      setTimeout(sendPendingDiff, 10);
    };

    quill.on('text-change', handler);
    quill.root.addEventListener('compositionstart', handleCompositionStart);
    quill.root.addEventListener('compositionend', handleCompositionEnd);
    return () => {
      quill.off('text-change', handler);
      quill.root.removeEventListener('compositionstart', handleCompositionStart);
      quill.root.removeEventListener('compositionend', handleCompositionEnd);
      if (pollTimer) clearInterval(pollTimer);
    };
  }, [sendDelta, seqNo, ws]);

  useEffect(() => {
    const quill = quillRef.current;
    if (!quill) return undefined;
    let timer = null;
    let pendingRange = null;
    const flush = () => {
      sendCursor(pendingRange);
      timer = null;
    };
    const handler = (range, _old, source) => {
      if (source !== 'user') return;
      pendingRange = range;
      if (timer) return;
      timer = setTimeout(flush, CURSOR_THROTTLE_MS);
    };
    quill.on('selection-change', handler);
    return () => {
      quill.off('selection-change', handler);
      if (timer) clearTimeout(timer);
    };
  }, [sendCursor, ws]);

  useEffect(() => {
    if (!connected) return;
    const cursors = quillRef.current?.getModule('cursors');
    cursors?.clearCursors();
  }, [connected]);

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
