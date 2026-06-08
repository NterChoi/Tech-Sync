# 배포 가이드 (Oracle Cloud ARM + Docker Compose)

Tech-Sync 프로덕션 배포 절차. 프론트(nginx) + 백엔드(Spring Boot) + DB 3종(MariaDB/MongoDB/Redis)을
단일 VM 인스턴스에서 `docker compose` 로 구동한다.

> **배포 대상: Oracle Cloud Always Free (VM.Standard.A1.Flex, ARM/aarch64).**
> Always Free 한도(최대 4 OCPU / 24GB RAM)라 스택 전체를 한 VM에 여유 있게 올린다.
> 모든 베이스 이미지가 arm64 멀티아키라 **Dockerfile/compose 수정 없이** VM 위에서 그대로 빌드된다.

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

## 사전 준비 (Oracle Cloud — Always Free)

1. **인스턴스 생성**
   - Shape: **VM.Standard.A1.Flex** (Ampere ARM). Always Free 한도 내 권장 **2 OCPU / 12GB RAM** (최대 4/24까지 무료)
   - 이미지: Canonical **Ubuntu 22.04 (aarch64)**
   - SSH 공개키 등록, 부팅 볼륨 기본(약 47GB)
   - ⚠️ Always Free A1은 리전별로 `Out of host capacity` 가 잦다 → 다른 가용영역(AD) 시도 또는 잠시 후 재시도

2. **네트워킹 — 2단계를 모두 열어야 한다 (Oracle 최대 함정)**
   - (a) VCN > Security List(또는 NSG) 인바운드 규칙 추가: `22`(SSH), `80`(HTTP), (HTTPS 시 `443`) — Source `0.0.0.0/0`, TCP
   - (b) **인스턴스 내부 iptables도 열어야 한다.** Oracle Ubuntu 이미지는 기본 iptables가 SSH 외 전부 차단한다:
     ```bash
     sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
     sudo netfilter-persistent save
     ```
     > 이걸 빠뜨리면 Security List 를 열어도 80 이 막힌 것처럼 보인다 (Oracle 1순위 트러블).

3. **Docker + Compose 설치**
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

- [ ] `http://<VM-public-IP>/` 접속 → 로그인 화면
- [ ] 회원가입 → 로그인 → 피드 표시
- [ ] 워크스페이스 생성 → 에디터 진입 → 실시간 편집(2개 브라우저)
- [ ] 커서 공유 표시
- [ ] (Phase 3 배포 후) SSE 알림 수신

## 알아둘 점 / 트러블슈팅

- **포트 80이 안 열림**: 십중팔구 위 2-(b) iptables 누락. `sudo iptables -L INPUT -n --line-numbers` 로
  80 ACCEPT 규칙이 SSH 위/근처에 있는지 확인. ufw 가 아니라 iptables 다.
- **아키텍처(ARM/arm64)**: Oracle A1은 ARM. 모든 베이스 이미지(temurin17, node20-alpine, nginx,
  mariadb/mongo/redis)가 arm64 멀티아키라 **Dockerfile 수정 불필요**. compose 가 VM 위에서
  `--build` 로 직접 빌드하므로 크로스아키텍처 문제 없음. (로컬 Windows/amd64에서 빌드한 이미지를
  옮기지 말고 반드시 VM 위에서 빌드할 것.)
- **HTTPS/wss**: 현재 HTTP 기준. 도메인 확보 시 nginx 앞단에 Let's Encrypt(certbot)를 두고
  `wss://` 로 전환한다. SockJS 는 HTTP/HTTPS 자동 대응.
- **첫 부팅 validate 실패**: `Schema-validation: missing table` 로그가 보이면 DDL_AUTO=update 누락.
- **SSE 가 끊김**: nginx `/api/alarm/subscribe` 블록의 `proxy_buffering off` 확인.
- **메모리**: A1 12GB+ 면 Mongo+Maria+JVM 동시 구동에 여유 충분 (스왑 불필요). 더 작은 VM(예: 1GB)을
  쓸 경우에만 스왑 2GB 추가 고려.
- **DB 포트**: 프로드 compose 는 DB 포트를 호스트에 노출하지 않는다(내부 네트워크 전용). 외부 DB
  접속이 필요하면 임시로 `ports` 추가.
