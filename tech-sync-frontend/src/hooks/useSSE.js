import { useEffect, useRef } from 'react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { tokenStore } from '../api/axios';

const SSE_URL = '/api/alarm/subscribe';

/**
 * 알림 SSE 구독 훅.
 * 브라우저 기본 EventSource 는 Authorization 헤더를 못 싣기 때문에
 * fetch 기반 @microsoft/fetch-event-source 로 Bearer 토큰을 전송한다.
 *
 * @param {(alarm: object) => void} onAlarm  'alarm' 이벤트 수신 시 콜백
 * @param {boolean} enabled  로그인 상태일 때만 연결
 */
export default function useSSE(onAlarm, enabled) {
  const onAlarmRef = useRef(onAlarm);
  onAlarmRef.current = onAlarm;

  useEffect(() => {
    if (!enabled) return undefined;
    const token = tokenStore.getAccess();
    if (!token) return undefined;

    const controller = new AbortController();

    fetchEventSource(SSE_URL, {
      signal: controller.signal,
      headers: { Authorization: `Bearer ${token}` },
      openWhenHidden: true, // 탭이 백그라운드여도 연결 유지
      onmessage(msg) {
        if (msg.event === 'alarm' && msg.data) {
          try {
            onAlarmRef.current?.(JSON.parse(msg.data));
          } catch {
            // 파싱 실패 무시
          }
        }
      },
      onerror(err) {
        // 던지면 재시도 중단되므로, 로깅만 하고 라이브러리 기본 백오프 재연결에 맡긴다
        console.warn('SSE 연결 오류, 재연결 시도:', err?.message ?? err);
      },
    });

    return () => controller.abort();
  }, [enabled]);
}
