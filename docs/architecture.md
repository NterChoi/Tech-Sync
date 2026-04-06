# 아키텍처

## 폴더 구조
```
tech-sync/
├── tech-sync-backend/
│   └── src/main/java/com/techsync/
│       ├── controller/   # WebSocket, SSE, REST 엔드포인트
│       ├── service/      # 비즈니스 로직, 협업 연산
│       ├── repository/   # MariaDB(JPA), MongoDB 레포지토리
│       └── config/       # Redis, Security, WebSocket 설정
├── tech-sync-frontend/
│   └── src/
│       ├── components/editor/  # Quill.js 에디터 컴포넌트
│       └── hooks/              # useSocket.js, useSSE.js
├── docker-compose.yml
├── CLAUDE.md
└── docs/
```

## 기술 스택
- Backend: Java 17, Spring Boot 3.x
- RDBMS: MariaDB — 회원, 키워드, 워크스페이스, 권한 관계형 데이터
- NoSQL: MongoDB — 기사, 스냅샷, 버전, 델타 로그 문서형 데이터
- Cache/Broker: Redis — Pub/Sub 메시지 브로커 + 세션 캐싱
- 실시간: WebSocket + STOMP (공동편집), SSE (알림)
- Frontend: React, Quill.js (Delta 포맷)

## 레이어 의존 방향
```
Controller → Service → Repository
               ↓
          (Redis / MongoDB / MariaDB)
```
Service는 다른 Service를 호출할 수 있으나, Controller는 Repository를 직접 호출하지 않는다.
