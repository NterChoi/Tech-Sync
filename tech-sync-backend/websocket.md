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
- 충돌(SEQ_NO 불일치) 발생 시 **서버 기준** OT 변환을 수행하고 클라이언트에 재전송한다
- DELTA_LOG는 삭제하지 않는다 (이력 추적 불변 원칙)
    - 이유: 삭제 시 OT 충돌 복구 시점을 잃게 되며, 문서 이력 감사(audit)가 불가능해진다

### 충돌 해결 전략 상세
```
1. 클라이언트 A가 Delta(seqNo=10) 전송
2. 서버가 현재 seqNo=12 확인 → 불일치 감지
3. 서버가 seqNo 10~12 사이의 DELTA_LOG를 조회
4. OT 변환: 클라이언트 A의 Delta를 서버 기준으로 변환
5. 변환된 Delta를 DELTA_LOG에 저장 (seqNo=13)
6. /topic/room/{roomId}로 변환된 Delta 브로드캐스트
```

### 3명 이상 동시 편집 시
- 각 클라이언트의 Delta는 도착 순서대로 처리 (FIFO)
- 동시 도착 시 서버의 수신 타임스탬프 기준으로 순서 결정
- 각 Delta는 이전의 모든 미반영 Delta에 대해 순차적 OT 변환

## STOMP 메시지 페이로드 최소화
```json
 // ✅ 올바른 예 — Delta만 전송
{ "roomId": "...", "delta": {...}, "seqNo": 42, "userId": "..." }

// ❌ 금지 — 전체 문서 내용 전송 (대역폭 낭비 + 충돌 해결 불가)
{ "roomId": "...", "fullContent": "..." }
```

### 페이로드 최대 크기
- 단일 Delta 메시지: 64KB 이하 권장
- 초과 시: Delta를 분할하여 여러 메시지로 전송

## 자동저장 로직
- 30초마다 `DRAFT_SNAPSHOT`을 upsert (insert or replace)
- upsert 키: `ROOM_ID` (단일 문서만 유지)
- 수동 저장 시에만 `DRAFT_VERSION` 신규 생성
- 이유: 자동저장은 작업 손실 방지 목적이므로 버전을 생성하지 않음. 버전 히스토리 오염 방지

### 자동저장 실패 시
- Redis에 임시 백업 후 다음 주기에 재시도
- 3회 연속 실패 시 클라이언트에 경고 메시지 전송 (/queue/user/{userId})

## 에이전트 작업 규칙

### EditorService 수정 시 필수 확인
1. DELTA_LOG 저장이 누락되지 않았는가
2. seqNo 채번이 원자적(atomic)인가 — MongoDB의 findAndModify 또는 Redis INCR 사용
3. 브로드캐스트 전에 Delta 변환이 완료되었는가 (변환 전 전송은 클라이언트 불일치 유발)
4. 자동저장 주기(30초)가 변경되지 않았는가 — 변경 시 반드시 사용자 승인

### 테스트 시 검증 포인트
- 동시 편집 시나리오: 2명이 같은 위치에 동시 입력 → 최종 문서 일관성
- 연결 끊김 후 재접속 → 누락된 Delta 보정
- 자동저장 upsert → 이전 스냅샷이 덮어씌워지는지 확인