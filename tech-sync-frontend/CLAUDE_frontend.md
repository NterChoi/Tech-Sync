# Frontend 규칙 (tech-sync-frontend)

## 컴포넌트 규칙
- 함수형 컴포넌트 + Hooks만 사용 (클래스형 금지)
- 파일명: PascalCase (`EditorPanel.jsx`)
- 커스텀 훅: `use` 접두사 (`useSocket.js`, `useSSE.js`)

## Quill.js Delta 규칙
Delta 연산은 반드시 순수 함수로 작성하고 직접 DOM 조작은 금지한다.
이유: DOM 직접 조작은 Quill의 내부 상태와 불일치를 만들어 OT 충돌 해결을 불가능하게 한다.

```js
// ✅ 올바른 예
const delta = new Delta().retain(5).insert('hello');
quill.updateContents(delta);

// ❌ 금지 — Quill 상태와 DOM 불일치 발생
document.querySelector('.ql-editor').innerText = '...';
```

## WebSocket 연결 패턴
```js
// useSocket.js 에서 STOMP 클라이언트 초기화
const client = new Client({
  brokerURL: `ws://${host}/ws`,
  onConnect: () => {
    client.subscribe(`/topic/room/${roomId}`, onDeltaReceived);
  },
  // 재연결 설정 — 네트워크 끊김 시 자동 복구
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});
```

### Delta 수신 시 처리 흐름
```js
function onDeltaReceived(message) {
  const { delta, seqNo, userId } = JSON.parse(message.body);

  // 1. 본인이 보낸 Delta는 무시 (이미 로컬 적용 완료)
  if (userId === currentUserId) return;

  // 2. SEQ_NO 검증 — 불일치 시 서버에서 보정된 Delta가 오므로 그대로 적용
  // 3. Quill에 적용
  quill.updateContents(delta, 'silent');
}
```

## SSE 연결 패턴
```js
// useSSE.js
const es = new EventSource('/api/alarm/subscribe', { withCredentials: true });
es.onmessage = (e) => dispatch(addAlarm(JSON.parse(e.data)));

// 연결 끊김 시 재연결 (EventSource는 기본적으로 자동 재연결)
es.onerror = (e) => {
  console.warn('SSE 연결 끊김, 자동 재연결 대기...');
};
```

## 환경변수
- `.env.local` — 로컬 개발용 (Git 제외)
- `REACT_APP_API_URL` — 백엔드 주소
- `REACT_APP_WS_URL` — WebSocket 주소

## 에이전트 작업 규칙

### 컴포넌트 작성 후 확인 사항
1. props에 대한 기본값 또는 PropTypes가 정의되어 있는가
2. useEffect 내 cleanup 함수가 있는가 (WebSocket/SSE 연결 해제)
3. 에러 상태 UI가 존재하는가 (로딩/에러/빈 데이터)

### 금지 패턴
- `useEffect` 의존성 배열 누락 (ESLint exhaustive-deps 규칙 준수)
- 컴포넌트 내부에서 직접 fetch/axios 호출 → 반드시 커스텀 훅으로 분리
- 인라인 스타일 남용 → CSS Module 또는 styled-components 사용