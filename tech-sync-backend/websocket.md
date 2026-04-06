---
paths:
  - "src/**/controller/*WebSocket*"
  - "src/**/controller/*Stomp*"
  - "src/**/service/*Collab*"
  - "src/**/service/*Editor*"
---

# WebSocket / 실시간 협업 규칙

## DELTA_LOG 처리 원칙
- 클라이언트에서 받은 Delta는 반드시 SEQ_NO 검증 후 적용한다
- 충돌(SEQ_NO 불일치) 발생 시 서버 기준 OT 변환을 수행하고 클라이언트에 재전송한다
- DELTA_LOG는 삭제하지 않는다 (이력 추적 불변 원칙)

## STOMP 메시지 페이로드 최소화
```json
// 올바른 예 — Delta만 전송
{ "roomId": "...", "delta": {...}, "seqNo": 42, "userId": "..." }

// 금지 — 전체 문서 내용 전송
{ "roomId": "...", "fullContent": "..." }
```

## 자동저장 로직
- 30초마다 `DRAFT_SNAPSHOT`을 upsert (insert or replace)
- upsert 키: `ROOM_ID` (단일 문서만 유지)
- 수동 저장 시에만 `DRAFT_VERSION` 신규 생성
