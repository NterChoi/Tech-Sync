# Tech-Sync 프로젝트

## 절대 규칙 (모든 작업에 우선)
- `.env`, DB 접속 정보, API Key를 코드에 노출하거나 커밋하지 않는다
- 실시간 충돌 해결 시 반드시 DELTA_LOG를 참조하여 OT 연산을 수행한다 (이유: DELTA_LOG는 불변 이력이며, 이를 무시하면 클라이언트 간 문서 불일치가 영구화된다)
- 로컬 개발 시 Production DB에 절대 직접 연결하지 않는다
- Controller → Service → Repository 레이어를 절대 건너뛰지 않는다 (이유: 트랜잭션·보안·캐싱 로직이 Service에 집중되어 있으므로, Controller→Repository 직접 호출은 이를 모두 우회한다)

## 아키텍처 요약 (상세: docs/architecture.md 참조)
- Backend: Java 17 + Spring Boot 3.x
- RDBMS: MariaDB — 회원, 키워드, 워크스페이스, 권한
- NoSQL: MongoDB — 기사, 스냅샷, 버전, 델타 로그
- Cache/Broker: Redis — Pub/Sub + 세션 캐싱 + Refresh Token
- 실시간: WebSocket+STOMP (공동편집), SSE (알림)
- Frontend: React + Quill.js (Delta 포맷)

## 레이어 의존 방향
```
Controller → Service → Repository
               ↓
          (Redis / MongoDB / MariaDB)
```
Service는 다른 Service를 호출할 수 있으나, Controller는 Repository를 직접 호출하지 않는다.

## 파일별 규칙 적용 범위
| 파일 | 적용 대상 |
|------|-----------|
| CLAUDE_root.md (이 파일) | 모든 작업 |
| progress.md | 새 대화 시작 시 반드시 참조 — 프로젝트 현재 상태, 완료/예정 작업, 설계 결정 이력 |
| exec-plan.md | 복잡한 작업의 실행 계획 수립 시 (2개 이상 파일 변경, 새 API, 스키마 변경 등) |
| CLAUDE_backend.md | tech-sync-backend/ 하위 모든 파일 |
| CLAUDE_frontend.md | tech-sync-frontend/ 하위 모든 파일 |
| websocket.md | src/**/controller/*WebSocket*, src/**/service/*Collab*, src/**/service/*Editor* |
| docs/architecture.md | 구조 변경이 필요할 때만 참조 (평소 로드 불필요) |
| docs/domain.md | 새 Entity/Document 작성 또는 스키마 변경 시에만 참조 |

> **충돌 시 우선순위**: CLAUDE_root.md > exec-plan.md > 도메인별 MD (backend/frontend/websocket) > docs/

## 환경 감지
현재 OS에 따라 아래 명령어를 자동 선택한다.
- Mac/Linux: `./gradlew`, 경로 구분자 `/`
- Windows: `gradlew.bat`, 경로 구분자 `\` (WSL 환경이면 Mac과 동일)

## 빌드 & 실행 명령어
```
빌드:        ./gradlew clean build
로컬 실행:   ./gradlew bootRun
테스트:      ./gradlew test
DB 기동:     docker compose up -d
DB 중단:     docker compose down
```

---

## 커스텀 커맨드

### `/test` — 테스트 실행 & 결과 보고
1. `./gradlew test` 실행
2. 실패한 테스트가 있으면 실패 목록과 에러 메시지 요약
3. 전체 통과 시 "✅ N개 테스트 통과" 출력
4. ❌ 실패 시: 수정 제안을 함께 제시하되, 자동 수정하지 않고 사용자 승인을 요청

### `/snapshot` — 스냅샷 로직 분석
1. DRAFT_SNAPSHOT 관련 코드 탐색
2. 현재 저장 구조와 MongoDB 스키마 분석
3. 최적화 제안 목록 출력
4. 적용 여부를 사용자에게 확인

### `/sync-check` — 환경 상태 점검
1. `docker ps`로 컨테이너 상태 확인
2. MariaDB, MongoDB, Redis 연결 테스트
3. `git status`로 미커밋 변경사항 확인
4. 결과를 한 눈에 보이도록 테이블 형태로 출력

### `배포해줘` — 단계별 승인 기반 배포
> ⚠️ 각 단계 완료 후 반드시 사용자 승인을 받은 뒤 다음 단계로 진행한다.

1. **테스트**: `./gradlew test` 실행 → 결과 보고
    - ❌ 실패 시: 여기서 중단. 실패 원인을 설명하고 수정 방안 제안
    - ✅ 통과 시: "테스트 통과. 빌드를 진행할까요?" → 사용자 승인 대기
2. **빌드**: `./gradlew clean build` 실행 → 결과 보고
    - ❌ 실패 시: 여기서 중단. 빌드 에러 분석
    - ✅ 성공 시: "빌드 성공. 커밋 메시지를 확인해주세요:" → 커밋 메시지 제안
3. **커밋**: 사용자가 승인한 메시지로 `git add . && git commit -m "메시지"`
    - 커밋 전 `git diff --staged` 요약을 보여줌
    - 사용자 승인 후 커밋 실행
4. **푸시**: "main 브랜치에 push합니다. 진행할까요?" → 최종 승인 후 `git push origin main`
5. 완료 요약 출력 (커밋 해시, 변경 파일 수, 배포 시간)

---

## 코딩 컨벤션
- Java: Google Java Style Guide, 들여쓰기 4칸
- REST: 리소스 복수형, 상태코드 명확히 (200·201·400·404·409·500)
- MongoDB: `_id` 외 비즈니스 키 별도 관리, 복합 인덱스 쿼리 패턴 고려
- WebSocket: 페이로드 최소화, `/topic/`(브로드캐스트) vs `/queue/`(1:1) 구분

---

## 에이전트 작업 규칙 (Harness Rules)

### 실행 계획 우선 원칙
복잡한 작업(2개 이상 파일 변경, 새 API, 스키마 변경, 실시간 로직 수정)은 코드 작성 전에 반드시 exec-plans.md의 템플릿에 따라 실행 계획을 제시하고 사용자 승인을 받는다. 상세 기준과 템플릿은 exec-plans.md를 참조한다.

### 코드 작성 후 자동 검증 루프
모든 코드 변경 작업 완료 후, 아래 검증을 자동 수행한다:

1. **컴파일 확인**: `./gradlew compileJava` — 실패 시 즉시 수정
2. **보안 체크리스트 자동 점검**:
    - API Key, DB 비밀번호가 코드에 하드코딩되어 있지 않은가
    - 민감 정보가 로그에 출력되지 않는가
3. **레이어 규칙 준수 확인**: Controller에서 Repository를 직접 호출하는 코드가 없는가

### 새 기능 구현 시 필수 산출물
새로운 Service 또는 Controller를 작성할 때, 반드시 함께 제공해야 하는 것:
- 대응하는 테스트 클래스 (최소 정상 케이스 1개 + 실패 케이스 1개)
- 테스트가 불가능한 환경이면 그 이유를 명시하고 수동 테스트 방법을 안내

### 변경 범위 제한
- 요청받지 않은 파일은 수정하지 않는다
- 리팩토링이 필요하다고 판단되면 수정하지 말고 제안만 한다
- 기존 테스트가 깨지는 변경은 사용자 승인 없이 진행하지 않는다