import { useEffect, useRef, useState, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import Quill from 'quill';
import { tokenStore } from '../api/axios';

const Delta = Quill.import('delta');

// Phase 1 클라이언트 OT: 서버는 채번/저장/relay만 수행하므로 동시 편집 시 인덱스 보정은 클라이언트가 담당한다.
// Tie-break 규칙: userId가 작은 쪽이 우선(priority) — 모든 클라이언트가 동일 규칙을 써야 수렴.
//   - 같은 위치 동시 insert 시 작은 userId의 글자가 앞에 놓이고, 큰 userId의 글자가 뒤로 밀린다.
// Phase 2(서버 OT)로 전환 시 이 transform 루프만 우회하면 된다 (서버가 변환된 ops를 보내주므로).
export default function useEditorSocket({
  workspaceId,
  currentUserId,
  onRemoteDelta,
  onRemoteCursor,
}) {
  const clientRef = useRef(null);
  const onRemoteDeltaRef = useRef(onRemoteDelta);
  const onRemoteCursorRef = useRef(onRemoteCursor);
  const pendingDeltasRef = useRef([]);
  const lastServerSeqNoRef = useRef(0);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    onRemoteDeltaRef.current = onRemoteDelta;
    onRemoteCursorRef.current = onRemoteCursor;
  });

  useEffect(() => {
    if (!workspaceId) return undefined;
    const accessToken = tokenStore.getAccess();
    if (!accessToken) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${accessToken}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        // 재연결 시 pending은 신뢰할 수 없으므로 비운다 (서버 인증 여부 불명).
        pendingDeltasRef.current = [];
        setConnected(true);
        setError(null);
        client.subscribe(
          `/topic/workspace/${workspaceId}/edit`,
          (frame) => {
            try {
              const broadcast = JSON.parse(frame.body);
              lastServerSeqNoRef.current = broadcast.seqNo;

              if (broadcast.userId === currentUserId) {
                // 자신의 echo: 이미 로컬에 반영됨. pending FIFO head pop만 수행.
                if (pendingDeltasRef.current.length > 0) {
                  pendingDeltasRef.current.shift();
                }
                return;
              }

              // 타인의 ops: pending에 대해 transform 후 quill에 적용.
              const selfHasPriority = currentUserId < broadcast.userId;
              let remoteDelta = new Delta(broadcast.ops);
              for (let i = 0; i < pendingDeltasRef.current.length; i++) {
                const p = pendingDeltasRef.current[i];
                const newRemote = p.transform(remoteDelta, selfHasPriority);
                pendingDeltasRef.current[i] = remoteDelta.transform(p, !selfHasPriority);
                remoteDelta = newRemote;
              }
              onRemoteDeltaRef.current?.({
                ...broadcast,
                ops: remoteDelta.ops,
              });
            } catch (e) {
              console.error('Failed to parse edit broadcast', e);
            }
          },
        );
        client.subscribe(
          `/topic/workspace/${workspaceId}/cursor`,
          (frame) => {
            try {
              const broadcast = JSON.parse(frame.body);
              if (broadcast.userId === currentUserId) return;
              onRemoteCursorRef.current?.(broadcast);
            } catch (e) {
              console.error('Failed to parse cursor broadcast', e);
            }
          },
        );
      },
      onStompError: (frame) => {
        setError(frame.headers?.message || 'STOMP 오류');
        setConnected(false);
      },
      onWebSocketClose: () => setConnected(false),
    });

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [workspaceId, currentUserId]);

  const sendDelta = useCallback(
    (ops) => {
      const client = clientRef.current;
      if (!client || !client.connected) return;
      pendingDeltasRef.current.push(new Delta(ops));
      client.publish({
        destination: `/app/edit/${workspaceId}`,
        body: JSON.stringify({ ops, clientSeqNo: lastServerSeqNoRef.current }),
      });
    },
    [workspaceId],
  );

  const sendCursor = useCallback(
    (range) => {
      const client = clientRef.current;
      if (!client || !client.connected) return;
      client.publish({
        destination: `/app/cursor/${workspaceId}`,
        body: JSON.stringify({ range }),
      });
    },
    [workspaceId],
  );

  return { connected, error, sendDelta, sendCursor };
}
