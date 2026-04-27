# Tech-Sync 진행 기록 (Progress)

이 문서는 프로젝트의 현재 상태와 진행 이력을 기록한다.
에이전트는 새 대화 시작 시 이 파일을 참조하여 프로젝트 맥락을 파악한다.
**작업 완료 시 에이전트는 이 파일을 업데이트하고 사용자에게 확인을 받는다.**

---

## 현재 상태

- 마지막 업데이트: 2026-04-27
- 현재 브랜치: `develop`
- 다음 작업: 2-2b WebSocket OT 변환 알고리즘 — `feature/websocket-ot`

---

## 완료된 작업

### Phase 1: 뉴스 피드 시스템

#### 1-1. feature/news-keyword-master ✅ (→ develop merge 완료)
- KEYWORD_MASTER 테이블 + DataInitializer (추천 키워드 초기 데이터)
- KeywordController, KeywordMasterRepository

#### 1-2. feature/news-rss-collector ✅ (→ develop merge 완료)
- 긱뉴스 RSS 수집기 (`feeds.feedburner.com/geeknews-feed`)
- 전체 저장, 공통 피드
- 수집 주기: 1시간마다 `@Scheduled`
- 중복 방지: `<id>` 기준

#### 1-3. feature/news-naver-api ✅ (→ develop merge 완료)
- 네이버 뉴스 API 연동
- 사용자 구독 키워드 기반 개인화
- 중복 방지: 링크 URL 기준

#### 1-4. feature/news-keyword-subscribe ✅ (→ develop merge 완료)
- 사용자별 키워드 구독 기능
- 관리자 제공 추천 키워드 선택 방식 (자유 입력 X)

#### 1-5. feature/news-feed-api ✅ (→ develop merge 완료, PR #5)

**구현 파일:**

| 파일 | 내용 |
|------|------|
| `domain/Scrap.java` | SCRAP 엔티티 (MariaDB, USER_ID + ARTICLE_ID 복합 unique) |
| `repository/ScrapRepository.java` | JPA repo, JPQL로 articleId 목록 조회 |
| `repository/ArticleRepository.java` | `@Query`로 GEEK 전체 + NAVER 구독키워드 통합 피드 쿼리 추가 |
| `dto/ArticleResponse.java` | 피드 응답 DTO (`@JsonProperty("isScraped")` 명시) |
| `service/FeedService.java` | 인터페이스 |
| `service/FeedServiceImpl.java` | 피드 조회 + 스크랩 CRUD 로직 |
| `controller/FeedController.java` | REST API |

**API 엔드포인트:**

| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/feed` | 통합 피드 조회 (GEEK 전체 + 구독 키워드 NAVER, 페이지네이션) |
| GET | `/api/feed/scraps` | 내 스크랩 목록 조회 |
| POST | `/api/feed/{articleId}/scrap` | 스크랩 추가 (201) |
| DELETE | `/api/feed/{articleId}/scrap` | 스크랩 해제 (200) |

**설계 결정사항:**
- 뉴스 소스: 긱뉴스 RSS (공통) + 네이버 뉴스 API (개인화)
- 저장소: MongoDB `ARTICLE` 컬렉션 (SOURCE 필드로 NAVER/GEEK 구분)
- 스크랩: ARTICLE에 직접 저장하지 않고 MariaDB `SCRAP` 테이블로 분리 (per-user 데이터)
- 피드 조회 시 사용자의 scrapedIds를 Set으로 가져와 `isScraped` 계산
- 스크랩 목록이 비면 MongoDB 쿼리 없이 즉시 빈 페이지 반환
- 키워드 방식: 관리자 제공 추천 키워드 선택 (자유 입력 X)

### Phase 2: 협업 워크스페이스

#### 2-1. feature/workspace-crud ✅ (→ develop merge 완료, PR #6)
- WORKSPACE 테이블 + WORKSPACE_MEMBER 테이블 (MariaDB)
- 워크스페이스 CRUD API (생성/목록조회/상세조회/수정/삭제)
- 멤버 관리 API (이메일 초대/제거/탈퇴)
- 권한 체계: OWNER / EDITOR / VIEWER
- OWNER만 수정/삭제/초대 가능, 본인 탈퇴 허용
- ID 참조 방식 (User Entity 변경 없음, 기존 패턴 유지)
- 서비스 단위 테스트 14개 작성 (정상 7 + 실패 7, Mockito)

**구현 파일:**

| 파일 | 내용 |
|------|------|
| `domain/Workspace.java` | WORKSPACE 엔티티 (이름, 소유자ID, Auditing) |
| `domain/WorkspaceMember.java` | WORKSPACE_MEMBER 엔티티 (WORKSPACE_ID+USER_ID 복합 unique) |
| `repository/WorkspaceRepository.java` | JPA Repository |
| `repository/WorkspaceMemberRepository.java` | JPA Repository (멤버 조회/존재 확인 쿼리) |
| `dto/WorkspaceRequest.java` | 생성/수정 요청 DTO |
| `dto/MemberInviteRequest.java` | 멤버 초대 요청 DTO (이메일 + 역할) |
| `dto/WorkspaceResponse.java` | 워크스페이스 응답 DTO |
| `dto/WorkspaceDetailResponse.java` | 상세 응답 DTO (멤버 목록 포함) |
| `dto/MemberResponse.java` | 멤버 응답 DTO |
| `service/WorkspaceService.java` | 인터페이스 |
| `service/WorkspaceServiceImpl.java` | 비즈니스 로직 (권한 검증 포함) |
| `controller/WorkspaceController.java` | REST Controller (7개 엔드포인트) |
| `test/service/WorkspaceServiceImplTest.java` | 서비스 단위 테스트 (14개, Mockito) |

**API 엔드포인트:**

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/workspaces` | 워크스페이스 생성 (OWNER 자동 등록) |
| GET | `/api/workspaces` | 내 워크스페이스 목록 |
| GET | `/api/workspaces/{id}` | 상세 조회 (멤버 목록 포함) |
| PUT | `/api/workspaces/{id}` | 이름 수정 (OWNER만) |
| DELETE | `/api/workspaces/{id}` | 삭제 (OWNER만) |
| POST | `/api/workspaces/{id}/members` | 멤버 초대 (OWNER만) |
| DELETE | `/api/workspaces/{id}/members/{userId}` | 멤버 제거/탈퇴 |

#### 2-2a. feature/websocket-infra ✅ (→ develop merge 완료, PR #7)
- WebSocket(STOMP) 기반 Quill Delta 메시지 처리 인프라
- DELTA_LOG MongoDB 컬렉션 + (workspaceId, seqNo) 복합 unique 인덱스
- STOMP CONNECT 프레임 JWT 검증 (Authorization Bearer 헤더)
- 워크스페이스 멤버 검증 (비-멤버 편집 403 차단)
- seqNo 채번: Redis INCR `delta:seq:{workspaceId}`
- 브로드캐스트: `/topic/workspace/{workspaceId}/edit`
- 서비스 단위 테스트 4개 (정상 2 + 실패 2)
- ROOM_ID → WORKSPACE_ID 용어 통일 (문서 4개)

**구현 파일:**

| 파일 | 내용 |
|------|------|
| `domain/DeltaLog.java` | MongoDB Document — workspaceId, seqNo, userId, ops, createdAt |
| `repository/DeltaLogRepository.java` | MongoRepository + seqNo 범위 조회 |
| `dto/DeltaMessage.java` | 클라이언트 수신 페이로드 (ops + clientSeqNo) |
| `dto/DeltaBroadcast.java` | 서버 브로드캐스트 페이로드 |
| `service/EditorService.java` / `EditorServiceImpl.java` | 멤버 검증 + seqNo 채번 + 저장 + 브로드캐스트 |
| `controller/EditorWebSocketController.java` | `@MessageMapping("/edit/{workspaceId}")` |
| `config/StompAuthChannelInterceptor.java` | CONNECT 프레임 JWT 검증 인터셉터 |
| `config/WebSocketConfig.java` | inbound channel 인터셉터 등록 (수정) |
| `test/service/EditorServiceImplTest.java` | 단위 테스트 (4개, Mockito) |

**설계 결정:**
- seqNo는 Delta(ops 배열) 단위로 증가 — Quill-delta OT 연산 단위와 일치
- 인증: STOMP CONNECT 프레임의 native Authorization Bearer 헤더 (업계 관례)
- ROOM_ID 표기를 WORKSPACE_ID로 통일하여 Workspace 엔티티와 일관성 확보

### Phase 0: 인프라 & 인증 ✅
- Spring Boot 3.x 프로젝트 초기 설정
- Docker Compose (MariaDB, MongoDB, Redis)
- JWT 인증 (Access + Refresh Token, Redis 저장)
- Spring Security 설정
- User Entity + AuthController + AuthService
- GlobalExceptionHandler + ApiResponse 공통 래퍼
- WebSocket + STOMP 설정
- Redis 설정

---

## 진행 예정 작업

### Phase 2: 협업 워크스페이스
- [x] 2-1. 워크스페이스 CRUD (WORKSPACE, WORKSPACE_MEMBER) — 2026-04-20 완료, 2026-04-23 테스트 추가, PR #6
- [x] 2-2a. WebSocket 편집 인프라 (DELTA_LOG + STOMP 인증) — 2026-04-27 완료, PR #7
- [ ] 2-2b. OT 변환 알고리즘 (clientSeqNo 불일치 시 변환)
- [ ] 2-2c. DRAFT_SNAPSHOT 자동저장 (30초)
- [ ] 2-2d. DRAFT_VERSION 수동 저장
- [ ] Quill.js 에디터 연동 (프론트엔드)

### Phase 3: 알림 시스템
- [ ] Redis Pub/Sub 이벤트 발행
- [ ] SSE 실시간 알림 (/api/alarm/subscribe)
- [ ] Web Push (VAPID) — 브라우저 미연결 시

### Phase 4: 프론트엔드
- [ ] React 프로젝트 초기 설정
- [ ] 로그인/회원가입 UI
- [ ] 뉴스 피드 UI
- [ ] 에디터 컴포넌트 (Quill.js)
- [ ] useSocket.js, useSSE.js 훅

---

## 설계 결정 이력

| 날짜 | 결정 | 이유 |
|------|------|------|
| 2026-04-13 | 스크랩을 ARTICLE이 아닌 별도 SCRAP 테이블로 분리 | per-user 데이터를 MongoDB 문서에 넣으면 문서 비대화 + 동시성 문제 |
| 2026-04-13 | 키워드를 자유 입력이 아닌 추천 선택 방식으로 | 검색 품질 일관성 + 관리 용이성 |
| 2026-04-13 | 긱뉴스는 전체 저장, 네이버는 구독 키워드 기반 | 긱뉴스는 IT 전문이라 전체가 유의미, 네이버는 범위가 넓어 필터링 필요 |
| 2026-04-27 | 편집방 식별자를 ROOM_ID가 아닌 WORKSPACE_ID로 통일 | Workspace 엔티티와 용어 일치, 별도 ROOM 개념 도입은 YAGNI |
| 2026-04-27 | seqNo는 Delta(ops 배열) 단위로 채번 | Quill-delta 라이브러리의 OT 연산 단위와 일치, 단일 op 분할 저장은 비효율 |
| 2026-04-27 | STOMP 인증을 CONNECT 프레임 Authorization 헤더로 처리 | 업계 관례, 핸드셰이크 단계에서 차단하여 비인증 메시지 발행 방지 |

---

## 업데이트 규칙

에이전트는 아래 시점에 이 파일을 업데이트한다:
- 브랜치 작업 완료 및 PR merge 시 → "완료된 작업"에 추가
- 새 설계 결정 시 → "설계 결정 이력"에 추가
- 다음 작업 계획 변경 시 → "진행 예정 작업" 수정
- 업데이트 후 반드시 사용자에게 변경 내용을 보여주고 확인을 받는다