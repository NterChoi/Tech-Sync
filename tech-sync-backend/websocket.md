---
paths:
  - "src/**/controller/*WebSocket*"
  - "src/**/controller/*Stomp*"
  - "src/**/service/*Collab*"
  - "src/**/service/*Editor*"
---

# WebSocket / 실시간 협업 규칙

## DELTA_LOG 처리 원칙
- DELTA_LOG는 삭제하지 않는다 (이력 추적 불변 원칙)
    - 이유: 삭제 시 OT 충돌 복구 시점을 잃게 되며, 문서 이력 감사(audit)가 불가능해진다
- 서버는 모든 ops를 seqNo 순서대로 DELTA_LOG에 보존한다 (Phase 1/2 공통)

### OT 위치: Phase 1(현재) — 클라이언트 OT
- 서버는 채번(Redis INCR) + DELTA_LOG 저장 + relay만 수행 (변환하지 않음)
- 서버는 `DeltaBroadcast.clientSeqNo`로 클라가 보낸 clientSeqNo를 echo (자신의 ack 식별용)
- 클라이언트는 quill-delta `transform()`으로 pending 큐 + 원격 ops를 보정 (`useEditorSocket.js` 참조)
- Tie-break: 작은 userId가 priority — 모든 클라가 동일 규칙으로 수렴 보장

채택 사유: Java용 quill-delta 호환 OT 라이브러리 부재 + 5/28 발표 일정. 시연 환경(2~3명)에선 충분히 동작.

### OT 위치: Phase 2(발표 후 전환 예정) — 서버 OT
- 서버가 권위적 OT 수행. 클라가 보낸 `clientSeqNo`와 현재 `seqNo` 사이의 DELTA_LOG를 transform 베이스로 사용
- 클라이언트의 transform 루프는 비활성화 (서버가 변환된 ops를 보내주므로 그대로 적용)
- 전환 시 DELTA_LOG 스키마/브로드캐스트 페이로드 변경 불필요 → 코드 변경 표면적 최소

### Phase 2 충돌 해결 전략 (참고용 — 미구현)
```
1. 클라이언트 A가 Delta(clientSeqNo=10) 전송
2. 서버가 현재 seqNo=12 확인 → 불일치 감지
3. 서버가 seqNo 11~12 사이의 DELTA_LOG를 조회
4. OT 변환: 클라이언트 A의 Delta를 서버 기준으로 변환
5. 변환된 Delta를 DELTA_LOG에 저장 (seqNo=13)
6. /topic/workspace/{workspaceId}/edit로 변환된 Delta 브로드캐스트
```

### 3명 이상 동시 편집 시 (Phase 1/2 공통)
- 서버는 STOMP 단일 세션 FIFO에 따라 수신 순서대로 seqNo 채번
- 브로드캐스트 순서는 seqNo 순서와 동일
- Phase 1: 각 클라가 자신의 pending 큐에 대해 순차 transform → 일관된 tie-break으로 수렴
- Phase 2: 서버가 transform 누적 적용

## STOMP 메시지 페이로드 최소화
```json
 // ✅ 올바른 예 — Delta만 전송
{ "workspaceId": 1, "delta": {...}, "seqNo": 42, "userId": 1 }

// ❌ 금지 — 전체 문서 내용 전송 (대역폭 낭비 + 충돌 해결 불가)
{ "workspaceId": 1, "fullContent": "..." }
```

### 페이로드 최대 크기
- 단일 Delta 메시지: 64KB 이하 권장
- 초과 시: Delta를 분할하여 여러 메시지로 전송

## 자동저장 로직
- 30초마다 `DRAFT_SNAPSHOT`을 upsert (insert or replace)
- upsert 키: `WORKSPACE_ID` (단일 문서만 유지)
- 수동 저장 시에만 `DRAFT_VERSION` 신규 생성
- 이유: 자동저장은 작업 손실 방지 목적이므로 버전을 생성하지 않음. 버전 히스토리 오염 방지

### 자동저장 실패 시
- Redis에 임시 백업 후 다음 주기에 재시도
- 3회 연속 실패 시 클라이언트에 경고 메시지 전송 (/queue/user/{userId})

## 에이전트 작업 규칙

### EditorService 수정 시 필수 확인
1. DELTA_LOG 저장이 누락되지 않았는가
2. seqNo 채번이 원자적(atomic)인가 — MongoDB의 findAndModify 또는 Redis INCR 사용
3. `DeltaBroadcast.clientSeqNo` echo가 누락되지 않았는가 (Phase 1 클라 OT 매칭에 필수)
4. 자동저장 주기(30초)가 변경되지 않았는가 — 변경 시 반드시 사용자 승인
5. Phase 2 전환 시: 4번 위에 "transform 누적 적용 후 브로드캐스트" 항목 추가

### 테스트 시 검증 포인트
- 동시 편집 시나리오: 2명이 같은 위치에 동시 입력 → 최종 문서 일관성
- 연결 끊김 후 재접속 → 누락된 Delta 보정
- 자동저장 upsert → 이전 스냅샷이 덮어씌워지는지 확인