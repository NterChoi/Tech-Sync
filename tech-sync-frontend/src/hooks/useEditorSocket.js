import { useEffect, useRef, useState, useCallback } from 'react';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import { tokenStore } from '../api/axios';

export default function useEditorSocket({
  workspaceId,
  currentUserId,
  onRemoteDelta,
  onRemoteCursor,
}) {
  const clientRef = useRef(null);
  const onRemoteDeltaRef = useRef(onRemoteDelta);
  const onRemoteCursorRef = useRef(onRemoteCursor);
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
        setConnected(true);
        setError(null);
        client.subscribe(
          `/topic/workspace/${workspaceId}/edit`,
          (frame) => {
            try {
              const broadcast = JSON.parse(frame.body);
              if (broadcast.userId === currentUserId) return;
              onRemoteDeltaRef.current?.(broadcast);
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
    (ops, clientSeqNo = 0) => {
      const client = clientRef.current;
      if (!client || !client.connected) return;
      client.publish({
        destination: `/app/edit/${workspaceId}`,
        body: JSON.stringify({ ops, clientSeqNo }),
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
