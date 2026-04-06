# Frontend 규칙 (tech-sync-frontend)

## 컴포넌트 규칙
- 함수형 컴포넌트 + Hooks만 사용 (클래스형 금지)
- 파일명: PascalCase (`EditorPanel.jsx`)
- 커스텀 훅: `use` 접두사 (`useSocket.js`, `useSSE.js`)

## Quill.js Delta 규칙
Delta 연산은 반드시 순수 함수로 작성하고 직접 DOM 조작은 금지한다.
```js
// 올바른 예
const delta = new Delta().retain(5).insert('hello');
quill.updateContents(delta);

// 금지
document.querySelector('.ql-editor').innerText = '...';
```

## WebSocket 연결 패턴
```js
// useSocket.js 에서 STOMP 클라이언트 초기화
const client = new Client({
  brokerURL: `ws://${host}/ws`,
  onConnect: () => {
    client.subscribe(`/topic/room/${roomId}`, onDeltaReceived);
  }
});
```

## SSE 연결 패턴
```js
// useSSE.js
const es = new EventSource('/api/alarm/subscribe', { withCredentials: true });
es.onmessage = (e) => dispatch(addAlarm(JSON.parse(e.data)));
```

## 환경변수
- `.env.local` — 로컬 개발용 (Git 제외)
- `REACT_APP_API_URL` — 백엔드 주소
- `REACT_APP_WS_URL` — WebSocket 주소
