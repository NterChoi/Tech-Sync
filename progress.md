# Tech-Sync 진행 기록 (Progress)

이 문서는 프로젝트의 현재 상태와 진행 이력을 기록한다.
에이전트는 새 대화 시작 시 이 파일을 참조하여 프로젝트 맥락을 파악한다.
**작업 완료 시 에이전트는 이 파일을 업데이트하고 사용자에게 확인을 받는다.**

---

## 현재 상태

- 마지막 업데이트: 2026-05-26
- 현재 브랜치: `develop` (OT 변환 Phase 1 PR #13 머지 완료, HEAD `bf42c8f`)
- 4/30 발표 완료. 5/28 3차 발표 대비 협업 워크스페이스 완성도 작업 진행 중 (D-2)
- 다음 작업: 2-2c DRAFT_SNAPSHOT 자동저장(30초 upsert) → 2-2d DRAFT_VERSION 수동 저장 → Phase 2 OT 마이그레이션(발표 후)

### 다음 세션 이어받기 메모 (2026-05-26 마무리, **다음 세션은 맥북에서 진행**)
- **머신 전환**: Windows에서 OT Phase 1 머지까지 완료. 다음 작업(2-2c)부터는 맥북에서. 맥북 시작 시:
  - `git fetch && git checkout develop && git pull`
  - `CLAUDE.local.md`의 `OS: mac` 확인 (.gitignore라 자동 동기화 안 됨, 머신별 관리)
  - `docker compose up -d` 로 MariaDB/MongoDB/Redis 기동
  - 새 의존성 0개 (quill-delta는 quill 번들 포함) — `npm install` / `./gradlew build`는 안전망으로만 한 번
- **2-2c 시작 시점에 결정 필요**: SNAPSHOT 범위
  - (a) 발표 시연용 최소: `DRAFT_SNAPSHOT` 30초 upsert + 페이지 진입 시 복원 — 가장 빠름, 시연 검증 가능
  - (b) 풀스펙: + Redis 임시 백업 + 3회 연속 실패 시 `/queue/user/{userId}` 경고 (websocket.md 명세)
  - D-2 + 2-2d/Phase 2 마이그레이션까지 남았으니 (a) 추천
- **OT Phase 2(서버 OT) 발표 후 마이그레이션 항목 신설** — "진행 예정 작업"에 추가됨
- **로컬 상태 주의 (Windows 머신 한정)**:
  - `stash@{0}`에 `build.gradle` 메모(`// 스프링 3부터 확인 필요`) 그대로 보관 — 맥북엔 영향 없음
  - dev 서버(8080 백엔드 + 3000 프론트)는 이 세션 끝에 정리

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

#### 2-2b. feature/ot-client-transform ✅ (→ develop merge 완료, PR #13, 2026-05-26)
- Phase 1 클라이언트 OT 변환 — 동시 편집 시 quill-delta `transform()`으로 인덱스 보정
- 서버는 채번 + DELTA_LOG 저장 + relay만 수행 (변환 없음)
- 클라가 `pendingDeltasRef` 큐 보유 → 원격 ops 도착 시 pending 각 항목에 대해 transform 누적 → 변환된 ops를 quill에 적용
- Tie-break: 작은 userId가 priority — 모든 클라가 동일 규칙으로 수렴 보장
- 자신의 echo 수신 시 pending FIFO head pop만 수행 (이미 로컬에 반영됨)
- 재연결 시 pending 큐 비움 (서버 인증 여부 불명)

**구현 파일:**

| 파일 | 내용 |
|------|------|
| `dto/DeltaBroadcast.java` | `clientSeqNo` echo 필드 추가 (자신의 ack 식별용) |
| `service/EditorServiceImpl.java` | `DeltaBroadcast.of(log, message.clientSeqNo())` 호출 변경 |
| `test/service/EditorServiceImplTest.java` | echo 검증 케이스 1개 추가 (총 7개) |
| `tech-sync-backend/websocket.md` | Phase 1(클라 OT) / Phase 2(서버 OT, 발표 후) 로드맵 명시 |
| `src/hooks/useEditorSocket.js` | pending 큐 + transform 통합, sendDelta 시그니처 단순화 (clientSeqNo 자동 추적) |
| `src/pages/WorkspaceEditorPage.jsx` | `seqNo` state 제거, `sendDelta(ops, seqNo)` → `sendDelta(ops)` |

**시연 검증 완료 (2026-05-26):**
- ✅ 같은 위치 동시 입력 → 양쪽 화면 동일 순서로 수렴
- ✅ 다른 위치 동시 입력 → 인덱스 어긋남 없음
- ✅ 한쪽 5글자 입력 후 다른 쪽 맨 앞에 1글자 삽입 → 5글자가 정확히 1만큼 밀림
- ✅ 한글 IME + 다른 쪽 영문 동시 입력 (5/11 fix 회귀 없음)
- ✅ 커서 공유 (5/18 기능 회귀 없음)

### Phase 4: 프론트엔드 MVP (발표용)

#### 4-1. feature/frontend-mvp ✅ (→ develop merge 완료, PR #8)
- 4/30 개발 중간 발표를 위한 발표용 프론트엔드 MVP
- 스택: **Vite + React 18 + MUI 5** + react-router-dom + axios + @stomp/stompjs + sockjs-client + quill
- 전체 시연 시나리오 동작 검증 완료

**구현 파일 (신규 25개):**

| 카테고리 | 파일 |
|----------|------|
| 셋업 | `package.json`, `vite.config.js`(8080 프록시), `index.html`, `src/main.jsx`, `src/theme.js`, `src/index.css` |
| API 클라이언트 | `src/api/axios.js` (JWT 인터셉터 + 자동 refresh), `auth.js`, `feed.js`, `keywords.js`, `workspaces.js` |
| 인증/라우팅 | `src/store/AuthContext.jsx`, `src/routes/ProtectedRoute.jsx` |
| 레이아웃/공통 | `src/components/Layout.jsx`(AppBar+Drawer), `ArticleCard.jsx` |
| 페이지 | `LoginPage`, `SignupPage`(자동 로그인), `FeedPage`(페이지네이션), `ScrapsPage`, `WorkspacesPage`, `WorkspaceDetailPage`(멤버 관리), `WorkspaceEditorPage`(Quill+STOMP) |
| 훅 | `src/hooks/useEditorSocket.js` (SockJS+STOMP, native Authorization 헤더) |

**시연 검증 완료 (2026-04-28):**
- ✅ 회원가입 → 자동 로그인 → /feed 이동
- ✅ 뉴스 피드 카드 표시 + 스크랩 토글 + 내 스크랩 목록
- ✅ 워크스페이스 생성/멤버 초대(이메일+역할)/탈퇴
- ✅ **실시간 공동 편집** — 한쪽 입력 → 다른 브라우저(시크릿 창) 즉시 반영

**머지 완료된 hotfix (PR #9, 2026-04-28):**
- WorkspaceEditorPage Quill 마운트 시점 보정 (loading 단계에서 ref 미부착으로 빈 페이지였음)
- axios 인터셉터에 403도 토큰 만료로 처리 (Spring Security stateless에서 401 대신 403 반환)

**발표 후로 미룬 항목 (의도적 deferred):**
- **커서 공유 (cursor presence)**: Google Docs 스타일 — 다른 사용자의 커서 위치/선택영역이 보이도록. quill-cursors 모듈 + 새 STOMP destination(`/app/cursor/{id}`, `/topic/workspace/{id}/cursor`) 필요. 백엔드/프론트 양쪽 작업이라 발표 후 별도 PR.

#### 4-2. feature/frontend-ime-composition ✅ (2026-05-11, 5/28 발표 대비)
- 한글 IME composition 중 글자 누락 문제 해결 (구글 독스 식 실시간 동기화)
- Quill 2가 composition 중 contents 업데이트를 차단하는 동작을 우회
- compositionstart ~ compositionend 사이 50ms 폴링으로 `quill.root.innerText`에서 직접 텍스트 추출 → Delta로 변환 → diff 송신
- compositionend 후엔 Quill 정식 처리에 맡겨 attributes(bold/italic 등) 복원
- 시연 검증: "아"만 입력해도 다른 브라우저에 즉시 표시, "안녕하세요 반갑습니다" 마지막 글자까지 정상 송신

#### 4-3. feature/cursor-presence ✅ (→ develop merge 완료, PR #11/#12, 2026-05-18 구현 / 2026-05-21 머지)
- Google Docs 스타일 커서 공유 (다른 사용자의 커서 위치/선택 영역 색상 표시)
- 새 STOMP destination: `/app/cursor/{workspaceId}` (C→S), `/topic/workspace/{id}/cursor` (S→C)
- `quill-cursors` 모듈 등록, userId 해시 기반 색상 결정 (`hsl(hue, 70%, 45%)`)
- 사용자 이름은 `WorkspaceDetailResponse.members[].userName`에서 프론트가 룩업 (백엔드 DB 조회 0회)
- 송신 throttle 80ms (마우스 드래그 시 selection-change 폭주 방지)
- 포커스 잃음(`range === null`) → `cursors.removeCursor` 호출로 상대 화면에서 제거
- 재연결 시 `cursors.clearCursors()`로 stale 커서 정리
- DELTA_LOG 미저장 (휘발성 + OT와 무관), Redis seqNo 채번 미사용

**구현 파일:**

| 파일 | 내용 |
|------|------|
| `dto/CursorMessage.java` | 수신 페이로드 (range: index, length) |
| `dto/CursorBroadcast.java` | 송신 페이로드 (workspaceId, userId, range) |
| `service/EditorService.java` / `EditorServiceImpl.java` | `broadcastCursor` 메서드 추가 (멤버 검증 후 토픽 발행) |
| `controller/EditorWebSocketController.java` | `@MessageMapping("/cursor/{workspaceId}")` 추가, 인증 검증 헬퍼로 추출 |
| `test/service/EditorServiceImplTest.java` | 커서 단위 테스트 3개 (정상/비멤버/포커스 잃음) |
| `src/hooks/useEditorSocket.js` | `/cursor` 구독 + `sendCursor(range)` 함수 노출 |
| `src/pages/WorkspaceEditorPage.jsx` | `Quill.register('modules/cursors')`, selection-change → throttled send, 원격 cursor 수신 처리, 재연결 시 clearCursors |

**시연 검증 완료 (2026-05-18):**
- ✅ 커서 위치 색상 표시 + 이름 라벨
- ✅ 드래그 선택 영역 표시
- ✅ 포커스 잃음 시 상대 화면에서 커서 사라짐
- ✅ 한글 IME 입력 중에도 커서/글자 동기화 정상

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
- [x] 2-2b. OT 변환 알고리즘 (Phase 1 클라이언트 OT) — 2026-05-26 완료, PR #13
- [ ] 2-2c. DRAFT_SNAPSHOT 자동저장 (30초)
- [ ] 2-2d. DRAFT_VERSION 수동 저장
- [ ] Quill.js 에디터 연동 (프론트엔드)

### 발표 후 (Phase 2 후속)
- [ ] OT Phase 2: 서버 OT 마이그레이션 — Java로 quill-delta `transform` 포팅 + `EditorServiceImpl.applyDelta`에서 transform 누적 적용 + 클라 transform 루프 비활성화. websocket.md "Phase 2 충돌 해결 전략" 참조. DELTA_LOG 스키마/페이로드 변경 불필요

### Phase 3: 알림 시스템
- [ ] Redis Pub/Sub 이벤트 발행
- [ ] SSE 실시간 알림 (/api/alarm/subscribe)
- [ ] Web Push (VAPID) — 브라우저 미연결 시

### Phase 4: 프론트엔드 MVP (발표용 — 4/30 발표)
- [x] React 프로젝트 초기 설정 (Vite + MUI) — 2026-04-27, PR #8
- [x] 로그인/회원가입 UI — PR #8
- [x] 뉴스 피드 UI + 스크랩 — PR #8
- [x] 워크스페이스 목록/상세/멤버 — PR #8
- [x] 에디터 컴포넌트 (Quill.js + STOMP) — PR #8 + hotfix
- [x] 한글 IME composition 이슈 해결 (50ms DOM 폴링 방식) — 2026-05-11, feature/frontend-ime-composition
- [x] 커서 공유 (quill-cursors + STOMP `/cursor` destination) — 2026-05-18 구현 / 2026-05-21 머지, PR #11/#12
- [ ] 키워드 구독 페이지 (NAVER 뉴스 데모용, nice-to-have)
- [ ] useSSE.js 훅 (Phase 3 알림 시스템 진행 시)

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
| 2026-04-27 | 발표용 프론트는 Vite + MUI로 (CRA/Tailwind 기각) | CRA deprecated, MUI는 3일 안에 깔끔한 화면 뽑기 가장 빠름 |
| 2026-04-27 | 토큰은 localStorage 저장 + axios 인터셉터로 자동 첨부 | 발표 데모 환경에서 XSS 위험은 비현실적, 단순함 우선 |
| 2026-04-27 | 백엔드 통신은 Vite proxy로 8080 우회 | CORS 설정 회피, dev 환경 단순화 |
| 2026-04-28 | axios 인터셉터에서 401뿐 아니라 403도 토큰 만료로 처리 | Spring Security stateless+JWT 환경에서 401 대신 403을 자주 던짐, _retry 플래그로 무한 루프 방지 |
| 2026-05-18 | 커서 메시지를 DELTA_LOG에 저장하지 않음 | 휘발성 데이터고 OT 변환과 무관, MongoDB 부하 + seqNo 채번 비용 회피 |
| 2026-05-18 | CursorBroadcast에 userName 미포함, 프론트가 멤버 목록에서 룩업 | 고빈도 메시지(throttle 80ms)마다 백엔드 User 조회를 피함, WorkspaceDetailResponse가 이미 멤버 이름을 들고 있음 |
| 2026-05-26 | OT 변환을 Phase 1(클라이언트 OT) → Phase 2(서버 OT) 2단계로 분리 | Java용 quill-delta 호환 transform 라이브러리 부재 + 5/28 발표 D-2. 서버는 채번/저장/relay 구조 그대로 유지하면 DELTA_LOG 스키마/페이로드 변경 없이 Phase 2 전환 가능 |
| 2026-05-26 | 클라 OT tie-break을 작은 userId priority로 결정 | 모든 클라가 동일한 결정론적 규칙을 사용해야 수렴 보장. 서버 seqNo 기반 tie-break은 pending이 미채번 상태라 불가 |
| 2026-05-26 | `DeltaBroadcast`에 `clientSeqNo` echo 필드 추가 | 자신의 ack 식별 + Phase 2 마이그레이션 시 서버가 transform 베이스로 활용 가능 (선제적 인터페이스) |

---

## 업데이트 규칙

에이전트는 아래 시점에 이 파일을 업데이트한다:
- 브랜치 작업 완료 및 PR merge 시 → "완료된 작업"에 추가
- 새 설계 결정 시 → "설계 결정 이력"에 추가
- 다음 작업 계획 변경 시 → "진행 예정 작업" 수정
- 업데이트 후 반드시 사용자에게 변경 내용을 보여주고 확인을 받는다