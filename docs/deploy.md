# 배포 가이드 (GCP Compute Engine + Docker Compose)

Tech-Sync 프로덕션 배포 절차. 프론트(nginx) + 백엔드(Spring Boot) + DB 3종(MariaDB/MongoDB/Redis)을
단일 VM 인스턴스에서 `docker compose` 로 구동한다.

> **배포 대상: GCP Compute Engine (e2-medium, x86_64, 서울 리전 asia-northeast3).**
> 신규 계정 $300 무료 크레딧(90일) 범위에서 운용. 4GB RAM 으로 스택 전체를 한 VM 에 올린다.
> 모든 베이스 이미지가 멀티아키라 **Dockerfile/compose 수정 없이** VM(amd64) 위에서 그대로 빌드된다.
>
> (이전 대상이던 Oracle Cloud Always Free(A1.Flex, ARM)는 `Out of host capacity` 로 인스턴스
> 확보 실패 → 2026-06-09 GCP 로 전환. 스택은 동일하게 단일 VM + docker compose.)

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

## 사전 준비 (GCP Compute Engine)

1. **인스턴스 생성** — gcloud CLI 또는 콘솔 둘 중 하나
   - 머신: **e2-medium** (2 vCPU / 4GB). e2-small(2GB)은 Mongo+Maria+JVM 동시 구동에 빠듯
   - 리전/존: **asia-northeast3-a (서울)** — 시연 지연 최소화
   - 이미지: **Ubuntu 22.04 LTS** (`ubuntu-2204-lts`, x86_64)
   - 부팅 디스크: 30GB
   - **방화벽: 생성 시 "HTTP 트래픽 허용" 체크** (= `http-server` 태그 + 80 포트 규칙 자동 추가). HTTPS 쓰면 443도.

   gcloud 예시:
   ```bash
   gcloud auth login                      # 브라우저 인증 (최초 1회)
   gcloud config set project <PROJECT_ID>

   gcloud compute instances create techsync \
     --zone=asia-northeast3-a \
     --machine-type=e2-medium \
     --image-family=ubuntu-2204-lts --image-project=ubuntu-os-cloud \
     --boot-disk-size=30GB \
     --tags=http-server
   ```

2. **(권장) 외부 IP 고정** — 기본 ephemeral IP 는 인스턴스 재시작 시 바뀐다. 시연 전 IP 가
   바뀌면 곤란하므로 static IP 예약을 권장한다.
   ```bash
   gcloud compute addresses create techsync-ip --region=asia-northeast3
   # 콘솔에서 인스턴스 > 네트워크 인터페이스 > 외부 IP 를 예약한 static 주소로 변경
   ```
   > GCP 의 방화벽은 VPC 레벨에서 처리되므로 **Oracle 처럼 인스턴스 내부 iptables 를 따로
   > 열 필요가 없다.** `http-server` 태그만 붙으면 80 이 바로 열린다.

3. **SSH 접속**
   ```bash
   gcloud compute ssh techsync --zone=asia-northeast3-a
   ```

4. **Docker + Compose 설치** (VM 안에서)
   ```bash
   sudo apt-get update && sudo apt-get install -y docker.io docker-compose-plugin
   sudo usermod -aG docker $USER   # 재로그인
   ```
5. 코드 가져오기: `git clone <repo> && cd Tech-Sync`
6. 환경변수 작성:
   ```bash
   cp .env.prod.example .env.prod
   vi .env.prod   # 실제 비밀번호/시크릿 입력. 첫 배포면 DDL_AUTO=update 주석 해제
   ```
   > JWT_SECRET 은 반드시 `openssl rand -base64 48` 형태의 Base64 여야 한다. 하이픈이 들어가면
   > `Illegal base64 character` 로 부팅 실패한다.

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

- [ ] `http://<VM-external-IP>/` 접속 → 로그인 화면
- [ ] 회원가입 → 로그인 → 피드 표시
- [ ] 워크스페이스 생성 → 에디터 진입 → 실시간 편집(2개 브라우저)
- [ ] 커서 공유 표시
- [ ] SSE 알림 수신 (멤버 초대 시 상대 벨 배지)

외부 IP 확인:
```bash
gcloud compute instances describe techsync --zone=asia-northeast3-a \
  --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

## 알아둘 점 / 트러블슈팅

- **포트 80이 안 열림**: GCP 는 VPC 방화벽이 게이트다. 인스턴스에 `http-server` 태그가 붙어
  있고 `default-allow-http`(또는 80 허용) 규칙이 있는지 확인:
  ```bash
  gcloud compute instances describe techsync --zone=asia-northeast3-a --format='get(tags.items)'
  gcloud compute firewall-rules list --filter="allowed[].ports:80"
  ```
  Oracle 과 달리 **인스턴스 내부 iptables 는 건드릴 필요 없다** (Ubuntu 기본 이미지가 80을 막지 않음).
- **아키텍처(amd64)**: GCP e2 는 x86_64. 모든 베이스 이미지(temurin17, node20-alpine, nginx,
  mariadb/mongo/redis)가 멀티아키라 **Dockerfile 수정 불필요**. compose 가 VM 위에서 `--build` 로
  amd64 이미지를 직접 빌드한다. (로컬에서 빌드한 이미지를 옮기지 말고 반드시 VM 위에서 빌드할 것.)
- **HTTPS/wss**: 현재 HTTP 기준. 도메인 확보 시 nginx 앞단에 Let's Encrypt(certbot)를 두고
  `wss://` 로 전환한다. SockJS 는 HTTP/HTTPS 자동 대응.
- **첫 부팅 validate 실패**: `Schema-validation: missing table` 로그가 보이면 DDL_AUTO=update 누락.
- **SSE 가 끊김**: nginx `/api/alarm/subscribe` 블록의 `proxy_buffering off` 확인.
- **메모리**: e2-medium 4GB 면 Mongo+Maria+JVM 동시 구동 가능. 빌드 중 OOM 이 보이면 스왑 2GB
  추가를 고려한다:
  ```bash
  sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
  ```
- **DB 포트**: 프로드 compose 는 DB 포트를 호스트에 노출하지 않는다(내부 네트워크 전용). 외부 DB
  접속이 필요하면 임시로 `ports` 추가.
- **비용**: e2-medium 은 $300 무료 크레딧 범위에서 운용된다. 시연 종료 후 인스턴스를 stop/delete
  하지 않으면 크레딧이 소진되므로 제출 후 정리할 것.
```
