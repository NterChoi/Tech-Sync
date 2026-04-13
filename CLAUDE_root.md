# Tech-Sync 프로젝트

@./docs/architecture.md
@./docs/domain.md

## 절대 규칙
- `.env`, DB 접속 정보, API Key를 코드에 노출하거나 커밋하지 않는다
- 실시간 충돌 해결 시 반드시 DELTA_LOG를 참조하여 OT 연산을 수행한다
- 로컬 개발 시 Production DB에 절대 직접 연결하지 않는다
- Controller → Service → Repository 레이어를 절대 건너뛰지 않는다

## 환경 감지
현재 OS에 따라 아래 명령어를 자동 선택한다.
- Mac: `./gradlew`, 경로 구분자 `/`
- Windows: `gradlew.bat`, 경로 구분자 `\` (또는 WSL 환경이면 Mac과 동일)

## 빌드 & 실행 명령어
```
빌드:        ./gradlew clean build
로컬 실행:   ./gradlew bootRun
테스트:      ./gradlew test
DB 기동:     docker compose up -d
DB 중단:     docker compose down
```

## 커스텀 커맨드
- `/test` — 전체 테스트 실행 후 결과 요약 보고
- `/snapshot` — 현재 스냅샷 로직과 MongoDB 저장 구조 분석 및 최적화 제안
- `/sync-check` — docker ps + DB 연결 상태 + 미커밋 변경사항 한 번에 점검
- `배포해줘` — 테스트 → 빌드 → Git commit & push (main) → 요약 출력

## 이어받기 메모

### 다음 세션 환경
- OS: **Windows** → `gradlew.bat` 사용, 경로 구분자 `\`
- WSL 환경이면 Mac과 동일하게 `./gradlew` 사용 가능
- CLAUDE.local.md 새로 생성 필요 (Windows 환경 맞게 작성)

### 2026-04-13 현재 상태
- 현재 브랜치: `feature/news-feed-api`
- `feature/news-keyword-master` PR → develop merge 완료
- `feature/news-rss-collector` PR → develop merge 완료
- `feature/news-naver-api` PR → develop merge 완료
- `feature/news-keyword-subscribe` PR → develop merge 완료
- `feature/news-feed-api` 구현 완료, PR 대기 중

#### 뉴스 피드 설계 결정사항
- 소스 1: 긱뉴스 RSS (`feeds.feedburner.com/geeknews-feed`) — 전체 저장, 공통 피드
- 소스 2: 네이버 뉴스 API — 사용자 구독 키워드 기반 개인화
- 수집 주기: 1시간마다 `@Scheduled`
- 키워드 방식: 관리자 제공 추천 키워드 선택 (자유 입력 X)
- 저장소: MongoDB `ARTICLE` 컬렉션 (SOURCE 필드로 NAVER/GEEK 구분)
- 중복 방지: 긱뉴스는 `<id>`, 네이버는 링크 URL 기준

#### 브랜치 작업 순서
1. ~~feature/news-keyword-master~~ 완료
2. ~~feature/news-rss-collector~~ 완료
3. ~~feature/news-naver-api~~ 완료
4. ~~feature/news-keyword-subscribe~~ 완료
5. ~~feature/news-feed-api~~ 구현 완료 (PR 머지 후 다음 단계로)

#### feature/news-feed-api 구현 내용
| 파일 | 내용 |
|------|------|
| `domain/Scrap.java` | SCRAP 엔티티 (MariaDB, USER_ID + ARTICLE_ID 복합 unique) |
| `repository/ScrapRepository.java` | JPA repo, JPQL로 articleId 목록 조회 |
| `repository/ArticleRepository.java` | `@Query`로 GEEK 전체 + NAVER 구독키워드 통합 피드 쿼리 추가 |
| `dto/ArticleResponse.java` | 피드 응답 DTO (`@JsonProperty("isScraped")` 명시) |
| `service/FeedService.java` | 인터페이스 |
| `service/FeedServiceImpl.java` | 피드 조회 + 스크랩 CRUD 로직 |
| `controller/FeedController.java` | REST API (아래 참고) |

#### 피드 API 엔드포인트
| Method | URL | 설명 |
|--------|-----|------|
| GET | `/api/feed` | 통합 피드 조회 (GEEK 전체 + 구독 키워드 NAVER, 페이지네이션) |
| GET | `/api/feed/scraps` | 내 스크랩 목록 조회 |
| POST | `/api/feed/{articleId}/scrap` | 스크랩 추가 (201) |
| DELETE | `/api/feed/{articleId}/scrap` | 스크랩 해제 (200) |

#### 스크랩 설계 결정사항
- ARTICLE 도큐먼트에 직접 저장하지 않고 MariaDB `SCRAP` 테이블로 분리 (per-user 데이터)
- 피드 조회 시 사용자의 scrapedIds를 Set으로 가져와 `isScraped` 필드 계산
- 스크랩 목록이 비면 MongoDB 쿼리 없이 즉시 빈 페이지 반환

#### 로컬 환경 참고
- JAVA_HOME: `C:/Users/8316-04/.jdks/jbr-17.0.14` (IntelliJ 내장 JDK)
- 빌드 명령: `JAVA_HOME="/c/Users/8316-04/.jdks/jbr-17.0.14" ./gradlew bootRun`
- `.env`는 루트와 `tech-sync-backend/` 양쪽에 있어야 함 (spring-dotenv가 실행 디렉토리 기준)
- curl 테스트 시 한글 포함 JSON은 쌍따옴표 이스케이프 방식(`\"`) 사용

#### 다음 작업 시 할 것
1. `feature/news-feed-api` → PR 생성 → develop merge
2. `git checkout develop && git pull origin develop`
3. WBS 다음 단계: **협업 워크스페이스** (5/12 예정) 또는 일정 앞당기기 검토

---

## 코딩 컨벤션
- Java: Google Java Style Guide, 들여쓰기 4칸
- REST: 리소스 복수형, 상태코드 명확히 (200·201·400·404·500)
- MongoDB: `_id` 외 비즈니스 키 별도 관리, 복합 인덱스 쿼리 패턴 고려
- WebSocket: 페이로드 최소화, `/topic/`(브로드캐스트) vs `/queue/`(1:1) 구분
