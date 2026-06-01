# 배포 가이드 (EC2 + Docker Compose)

Tech-Sync 프로덕션 배포 절차. 프론트(nginx) + 백엔드(Spring Boot) + DB 3종(MariaDB/MongoDB/Redis)을
단일 EC2 인스턴스에서 `docker compose` 로 구동한다.

## 구성 개요

```
[브라우저] ──80──> nginx(frontend 컨테이너)
                      ├─ /            → 정적 SPA (dist)
                      ├─ /api/        → backend:8080
                      ├─ /api/alarm/subscribe → backend:8080 (SSE, 버퍼링 off)
                      └─ /ws/         → backend:8080 (WebSocket/SockJS)
backend ──> mariadb:3306 / mongodb:27017 / redis:6379  (내부 네트워크 techsync-net)
```

프론트의 axios(`/api`)·SockJS(`/ws`)가 모두 상대경로라 nginx 가 같은 오리진에서 프록시한다.
→ 별도 CORS 설정 불필요, 프론트 코드 변경 없음.

## 사전 준비 (EC2)

1. 인스턴스 생성 (Ubuntu 22.04+, t3.small 이상 권장 — Mongo+Maria+JVM 메모리)
2. 보안그룹 인바운드: `22`(SSH), `80`(HTTP), (HTTPS 적용 시 `443`)
3. Docker + Compose 플러그인 설치
   ```bash
   sudo apt-get update && sudo apt-get install -y docker.io docker-compose-plugin
   sudo usermod -aG docker $USER   # 재로그인
   ```
4. 코드 가져오기: `git clone <repo> && cd Tech-Sync`
5. 환경변수 작성:
   ```bash
   cp .env.prod.example .env.prod
   vi .env.prod   # 실제 비밀번호/시크릿 입력. 첫 배포면 DDL_AUTO=update 주석 해제
   ```

## 배포

```bash
# 첫 배포 (스키마 생성): .env.prod 에 DDL_AUTO=update 설정한 상태
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build

# 기동 확인
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

정상 기동(테이블 생성 완료)을 확인했으면:

```bash
# .env.prod 에서 DDL_AUTO 줄을 다시 주석 처리(=validate)한 뒤 backend 만 재기동
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d backend
```

이후 코드 업데이트 배포:
```bash
git pull
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

## 동작 확인 체크리스트

- [ ] `http://<EC2-IP>/` 접속 → 로그인 화면
- [ ] 회원가입 → 로그인 → 피드 표시
- [ ] 워크스페이스 생성 → 에디터 진입 → 실시간 편집(2개 브라우저)
- [ ] 커서 공유 표시
- [ ] (Phase 3 배포 후) SSE 알림 수신

## 알아둘 점 / 트러블슈팅

- **HTTPS/wss**: 현재 HTTP 기준. 도메인 확보 시 nginx 앞단에 Let's Encrypt(certbot) 또는
  ALB 를 두고 `wss://` 로 전환한다. SockJS 는 HTTP/HTTPS 자동 대응.
- **첫 부팅 validate 실패**: `Schema-validation: missing table` 로그가 보이면 DDL_AUTO=update 누락.
- **SSE 가 끊김**: nginx `/api/alarm/subscribe` 블록의 `proxy_buffering off` 확인.
- **메모리 부족(OOM)**: 프리티어(t2.micro, 1GB)는 Mongo+Maria+JVM 동시 구동 시 부족할 수 있음 →
  스왑 2GB 추가 또는 t3.small 사용.
- **DB 포트**: 프로드 compose 는 DB 포트를 호스트에 노출하지 않는다(내부 네트워크 전용). 외부 DB
  접속이 필요하면 임시로 `ports` 추가.
