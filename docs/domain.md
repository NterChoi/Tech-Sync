# 도메인 컨텍스트

## MariaDB 테이블
| Table ID           | 설명               | 핵심 컬럼 |
|--------------------|--------------------|-----------|
| USER_INFO          | 회원 정보          | USER_ID(PK), USER_EMAIL, USER_PWD, USER_NAME |
| KEYWORD            | 구독 키워드        | KEYWORD_ID(PK), USER_ID(FK), KEYWORD_NAME |
| WORKSPACE          | 워크스페이스       | WORKSPACE_ID(PK), WORKSPACE_NAME, OWNER_ID(FK) |
| WORKSPACE_MEMBER   | 워크스페이스 멤버  | WORKSPACE_ID+USER_ID(복합Unique), MEMBER_ID(PK), ROLE |
| PUSH_SUBSCRIPTION  | Web Push 구독      | SUBSCRIPTION_ID(PK), ENDPOINT, P256DH_KEY, AUTH_KEY |
| KEYWORD_MASTER     | 추천 키워드 목록   | KEYWORD_ID(PK), KEYWORD_NAME, CATEGORY |

## MongoDB 컬렉션
| Collection      | 설명 | 핵심 필드 |
|-----------------|------|-----------|
| ARTICLE         | 뉴스 기사 + 스크랩·메모 | ARTICLE_ID, USER_ID, KEYWORD, IS_SCRAPED |
| DRAFT_SNAPSHOT  | 30초 자동저장 (단일 문서 upsert) | WORKSPACE_ID(PK), CONTENT, LAST_EDITOR_ID |
| DRAFT_VERSION   | 수동 저장 체크포인트 | VERSION_ID, WORKSPACE_ID, VERSION_NO, VERSION_TYPE(MAJOR/MINOR) |
| DELTA_LOG       | OT 충돌 해결의 핵심 — Quill Delta(ops 배열) 단위 append-only 로그 | WORKSPACE_ID, SEQ_NO, USER_ID, OPS(List\<retain/insert/delete\>), CREATED_AT |

## 실시간 플로우
```
클라이언트 편집
  → WebSocket(STOMP) /app/edit
  → EditorService.applyDelta()
  → DELTA_LOG 저장 (MongoDB)
  → OT 연산으로 충돌 해결
  → /topic/workspace/{workspaceId}/edit 브로드캐스트
  → 30초마다 DRAFT_SNAPSHOT upsert (자동저장)
```

## 알림 플로우
```
이벤트 발생 (뉴스 수집, 멘션, 초대)
  → Redis Pub/Sub 발행
  → AlarmService 구독
  → 브라우저 연결 여부 확인
    ├─ 연결됨: SSE /api/alarm/subscribe 전송
    └─ 미연결: Web Push (VAPID) 전송
```
