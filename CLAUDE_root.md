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

## 코딩 컨벤션
- Java: Google Java Style Guide, 들여쓰기 4칸
- REST: 리소스 복수형, 상태코드 명확히 (200·201·400·404·500)
- MongoDB: `_id` 외 비즈니스 키 별도 관리, 복합 인덱스 쿼리 패턴 고려
- WebSocket: 페이로드 최소화, `/topic/`(브로드캐스트) vs `/queue/`(1:1) 구분
