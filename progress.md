# Tech-Sync 진행 기록 (Progress)

이 문서는 프로젝트의 현재 상태와 진행 이력을 기록한다.
에이전트는 새 대화 시작 시 이 파일을 참조하여 프로젝트 맥락을 파악한다.
**작업 완료 시 에이전트는 이 파일을 업데이트하고 사용자에게 확인을 받는다.**

---

## 현재 상태

- 마지막 업데이트: 2026-04-14
- 현재 브랜치: `develop`
- 다음 작업: 협업 워크스페이스 단계 진입 (Phase 2)

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

### Phase 2: 협업 워크스페이스 (WBS 예정: 5/12~)
- [ ] 워크스페이스 CRUD (WORKSPACE, WORKSPACE_MEMBER)
- [ ] Quill.js 에디터 연동
- [ ] WebSocket 공동 편집 (DELTA_LOG + OT 연산)
- [ ] DRAFT_SNAPSHOT 자동저장 (30초)
- [ ] DRAFT_VERSION 수동 저장

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

---

## 업데이트 규칙

에이전트는 아래 시점에 이 파일을 업데이트한다:
- 브랜치 작업 완료 및 PR merge 시 → "완료된 작업"에 추가
- 새 설계 결정 시 → "설계 결정 이력"에 추가
- 다음 작업 계획 변경 시 → "진행 예정 작업" 수정
- 업데이트 후 반드시 사용자에게 변경 내용을 보여주고 확인을 받는다