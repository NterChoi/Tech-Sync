# 발표 Q&A 대비 — 이해용 정리

> 이 문서는 PPT에 넣는 것이 아니라, 발표 전에 읽고 머릿속에 넣어두는 용도입니다.
> 각 질문에 대해 **"왜?"까지 설명할 수 있으면** 발표에서 어떤 질문이 와도 대응 가능합니다.

---

## Part 1. Spring Security + JWT 관련

---

### Q1. "Spring Security 필터 체인이 뭔가요? JwtFilter는 어디에 위치하나요?"

**핵심 개념:**

Spring Security는 HTTP 요청이 Controller에 도달하기 전에 **여러 개의 필터를 순서대로 통과**시킵니다.
이걸 "필터 체인(Filter Chain)"이라고 합니다.

```
[HTTP 요청]
    ↓
┌─────────────────────────────────────────┐
│          Spring Security 필터 체인         │
│                                         │
│  1. CorsFilter (CORS 처리)               │
│  2. CsrfFilter (CSRF 방어) ← 우리는 OFF  │
│  3. ★ JwtFilter (우리가 추가한 것)         │
│  4. UsernamePasswordAuthenticationFilter │
│  5. ExceptionTranslationFilter           │
│  6. AuthorizationFilter (경로 인가)       │
│                                         │
└─────────────────────────────────────────┘
    ↓
[Controller]
```

**우리 프로젝트에서의 설정:**

```java
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
```

이 코드의 의미: "UsernamePasswordAuthenticationFilter **앞에** 우리의 JwtFilter를 넣어라."

**왜 앞에 넣나?**

Spring 기본 인증 필터(UsernamePasswordAuthenticationFilter)는 폼 로그인(아이디/비밀번호 전송)을
처리하는 필터입니다. 우리는 폼 로그인이 아니라 JWT를 쓰니까, 그 필터가 실행되기 **전에**
JWT를 먼저 검증해서 SecurityContext에 인증 정보를 넣어둡니다.

**이렇게 대답하세요:**

> "Spring Security는 필터 체인 구조로 동작합니다.
> 우리 JwtFilter는 Spring 기본 인증 필터 앞에 등록해서,
> 매 요청마다 Authorization 헤더의 JWT를 먼저 검증합니다.
> 검증이 성공하면 SecurityContext에 인증 객체를 세팅하고,
> 이후 필터들은 이미 인증된 사용자로 인식합니다."

---

### Q2. "CSRF를 왜 비활성화했나요? 보안 문제 없나요?"

**CSRF 공격이 뭔지 먼저 이해:**

CSRF(Cross-Site Request Forgery)는 이런 공격입니다:

1. 사용자가 은행 사이트에 로그인한 상태 (세션 쿠키가 브라우저에 있음)
2. 해커가 만든 악성 사이트를 방문함
3. 악성 사이트에 숨겨진 폼이 자동으로 은행에 "송금" 요청을 보냄
4. 브라우저는 자동으로 은행 세션 쿠키를 같이 보냄 → 은행은 정상 요청으로 인식

**핵심: CSRF는 "쿠키가 자동으로 첨부되는 것"을 악용하는 공격입니다.**

**왜 JWT에서는 CSRF가 불필요한가:**

우리 프로젝트는 JWT를 **쿠키가 아니라 Authorization 헤더**에 넣어서 보냅니다.

```
Authorization: Bearer eyJhbGci...
```

이 헤더는 JavaScript 코드가 **명시적으로** 넣어주는 것입니다.
악성 사이트에서 다른 도메인으로 요청을 보낼 때, 이 헤더를 임의로 추가할 수 없습니다.
(브라우저의 Same-Origin Policy가 막습니다)

→ 쿠키 자동 첨부가 없으므로 CSRF 공격 자체가 성립하지 않음 → CSRF 방어가 불필요

**이렇게 대답하세요:**

> "CSRF는 쿠키가 자동 전송되는 것을 악용하는 공격인데,
> 저희는 JWT를 쿠키가 아닌 Authorization 헤더에 담아 전송합니다.
> 헤더는 JavaScript에서 명시적으로 추가하는 것이라 자동 전송이 되지 않아서
> CSRF 공격이 성립하지 않습니다. 그래서 비활성화해도 보안 문제가 없습니다."

---

### Q3. "HMAC-SHA 대신 RSA를 쓰면 뭐가 다른가요?"

**두 방식의 차이:**

| | HMAC-SHA (우리 프로젝트) | RSA |
|---|---|---|
| 키 종류 | **대칭키** — 하나의 Secret Key | **비대칭키** — Public Key + Private Key |
| 서명 | Secret Key로 서명 | Private Key로 서명 |
| 검증 | **같은** Secret Key로 검증 | **다른** Public Key로 검증 |
| 적합한 상황 | 서명과 검증을 **같은 서버**가 하는 경우 | 서명하는 곳과 검증하는 곳이 **다른** 경우 |
| 성능 | 빠름 | 느림 |

**우리가 HMAC-SHA를 선택한 이유:**

Tech-Sync는 백엔드 서버가 1대입니다. 토큰을 **만드는 곳(로그인)**과 **검증하는 곳(JwtFilter)**이
같은 서버 안에 있습니다. 따라서 하나의 키로 충분합니다.

**RSA가 필요한 경우 예시:**

마이크로서비스에서 인증 서버가 토큰을 발급하고, 여러 다른 서비스가 검증해야 하는 경우.
Private Key는 인증 서버만 갖고 있고, Public Key만 다른 서비스에 배포하면
Secret Key 유출 위험 없이 검증할 수 있습니다.

**이렇게 대답하세요:**

> "HMAC-SHA는 대칭키 방식이라 하나의 시크릿 키로 서명과 검증을 둘 다 합니다.
> RSA는 비대칭키 방식이라 Private Key로 서명하고 Public Key로 검증합니다.
> 저희 프로젝트는 단일 서버라서 서명과 검증이 같은 서버에서 이루어지기 때문에
> 더 빠르고 단순한 HMAC-SHA를 선택했습니다.
> 마이크로서비스처럼 서버가 여러 대면 RSA가 더 적합합니다."

---

### Q4. "토큰이 탈취되면 어떻게 방어하나요?"

**Access Token이 탈취된 경우:**

Access Token은 만료 시간이 **30분**입니다.
탈취되더라도 30분이 지나면 자동으로 무효화됩니다.
이것이 Access Token의 수명을 짧게 설정한 이유입니다.

**Refresh Token이 탈취된 경우 — 여기가 핵심:**

우리 프로젝트는 **Refresh Token Rotation**을 적용합니다.

```
[정상 사용자의 갱신 요청]
   Refresh Token A로 갱신 요청
   → 서버: Access Token B + Refresh Token B 발급
   → 서버: Redis에 Refresh Token B 저장 (A는 자동 덮어쓰기로 무효화)

[해커가 탈취한 A로 갱신 시도]
   Refresh Token A로 갱신 요청
   → 서버: Redis에 저장된 값은 B임 → A ≠ B → 거부!
```

**추가 방어: Redis 저장**

Refresh Token을 Redis에 저장하고 있으므로,
이상 징후가 발견되면 해당 사용자의 Redis 키를 **삭제**하는 것만으로
즉시 모든 Refresh Token을 무효화할 수 있습니다. (= 강제 로그아웃)

**이렇게 대답하세요:**

> "두 가지 방어 전략이 있습니다.
> 첫째, Access Token은 수명을 30분으로 짧게 설정해서 탈취되더라도 피해 시간을 제한합니다.
> 둘째, Refresh Token은 Rotation 방식을 적용해서,
> 갱신할 때마다 새 Refresh Token을 발급하고 이전 것은 Redis에서 덮어씁니다.
> 탈취된 이전 토큰으로 갱신을 시도하면 Redis 저장값과 불일치해서 거부됩니다.
> 추가로 Redis 키를 삭제하면 강제 로그아웃도 가능합니다."

---

### Q5. "Refresh Token을 왜 DB가 아니라 Redis에 저장하나요?"

**Redis의 특성:**

- **인메모리** 저장소 → 읽기/쓰기가 매우 빠름 (마이크로초 단위)
- **TTL(Time To Live)** 기능 → 키에 만료 시간을 설정하면 자동으로 삭제됨
- **키-값 구조** → Refresh Token처럼 단순 조회에 최적

**왜 MariaDB가 아닌가:**

| | Redis | MariaDB |
|---|---|---|
| 조회 속도 | ~0.1ms | ~1-5ms |
| TTL 자동 삭제 | 지원 (설정만 하면 됨) | 미지원 (별도 배치 필요) |
| 적합한 데이터 | 임시 데이터, 캐시, 세션 | 영구 데이터, 관계형 데이터 |

Refresh Token은:
- 매 API 요청마다 조회될 수 있음 (토큰 갱신 시) → **빠른 속도 필요**
- 7일 후 자동 만료되어야 함 → **TTL이 편리**
- 영구 보관할 필요 없음 → DB에 쌓일 이유 없음

**이렇게 대답하세요:**

> "Refresh Token은 7일 후 자동 만료되는 임시 데이터입니다.
> Redis는 인메모리라 조회가 매우 빠르고, TTL 기능으로 만료 시간이 지나면 자동 삭제됩니다.
> MariaDB에 저장하면 만료된 토큰을 삭제하는 별도 배치 작업이 필요하고 속도도 느립니다.
> 그래서 임시 데이터에 최적화된 Redis를 선택했습니다."

---

### Q6. "BCrypt가 뭔가요? 왜 SHA-256 같은 해시를 안 쓰나요?"

**일반 해시(SHA-256)의 문제:**

SHA-256은 **동일한 입력 → 항상 동일한 출력**입니다.

```
"password123" → SHA-256 → "ef92b778bafe771e89245b..."
```

해커가 흔한 비밀번호 수백만 개를 미리 해시해둔 테이블을 만들 수 있습니다.
이걸 **레인보우 테이블 공격**이라고 합니다.

**BCrypt가 해결하는 방법:**

1. **Salt(소금)**: 비밀번호마다 **랜덤 값을 섞어서** 해시합니다.
   같은 "password123"이라도 매번 다른 해시값이 나옵니다.
   → 레인보우 테이블 무효화

2. **Cost Factor(비용 계수)**: 의도적으로 해시 계산을 **느리게** 만듭니다.
   일반 해시는 초당 수십억 번 계산 가능하지만, BCrypt는 초당 수백 번.
   → 무차별 대입(Brute Force) 공격 비용이 엄청나게 올라감

```
BCrypt 해시값 예시:
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 ↑   ↑   ↑─────── Salt ───────↑──── Hash ────↑
 알고리즘 Cost(10)
```

**이렇게 대답하세요:**

> "SHA-256 같은 일반 해시는 같은 입력이면 항상 같은 출력이라
> 레인보우 테이블 공격에 취약합니다.
> BCrypt는 매번 랜덤 Salt를 섞어서 같은 비밀번호도 다른 해시값이 나오고,
> 의도적으로 계산 속도를 느리게 해서 무차별 대입 공격도 방어합니다.
> Spring Security에서도 BCrypt를 기본 권장하고 있습니다."

---

### Q7. "Stateless가 뭔가요? 세션 방식과 정확히 뭐가 다른가요?"

**Stateful (세션 방식):**

```
[로그인 시]
클라이언트 → 서버: 아이디/비밀번호
서버 → 서버 메모리: 세션 생성 (sessionId=abc123, userId=1)
서버 → 클라이언트: 쿠키에 sessionId=abc123 저장

[이후 요청]
클라이언트 → 서버: 쿠키(sessionId=abc123)
서버: 메모리에서 abc123 찾음 → userId=1 확인 → 인증 완료
```

서버가 **"이 사용자가 로그인했다"는 상태를 기억**하고 있어야 합니다.

문제:
- 서버가 2대면? → 서버 A에서 만든 세션을 서버 B는 모름
- 서버가 재시작되면? → 메모리의 세션이 날아감
- 사용자가 10만 명이면? → 10만 개의 세션을 메모리에 들고 있어야 함

**Stateless (JWT 방식):**

```
[로그인 시]
클라이언트 → 서버: 아이디/비밀번호
서버: JWT 생성 (userId=1, 만료시간, 서명)
서버 → 클라이언트: JWT 전달

[이후 요청]
클라이언트 → 서버: Authorization: Bearer {JWT}
서버: JWT의 서명 검증 + 만료 확인 → userId=1 추출 → 인증 완료
```

서버가 **아무것도 기억하지 않습니다.** 토큰 자체에 정보가 들어있으니까요.

- 서버가 2대? → 같은 Secret Key만 공유하면 아무 서버나 검증 가능
- 서버 재시작? → 상관없음, 토큰은 클라이언트가 들고 있음
- 사용자 10만 명? → 서버 메모리 부담 없음

**이렇게 대답하세요:**

> "Stateful은 서버가 세션 메모리에 로그인 상태를 저장하는 방식이고,
> Stateless는 서버가 아무 상태도 저장하지 않는 방식입니다.
> JWT에는 사용자 정보가 토큰 자체에 들어있어서
> 서버는 토큰의 서명만 검증하면 됩니다.
> 덕분에 서버를 여러 대로 늘려도 세션 공유 문제가 없고,
> 저희 프로젝트처럼 HTTP와 WebSocket을 동시에 쓰는 환경에 적합합니다."

---

## Part 2. 추가 개발 관련

---

### Q8. "OT 변환이 정확히 어떻게 동작하나요?"

**구체적인 예시로 이해:**

초기 문서: `"ABCDE"` (5글자)

User A (userId=1): 2번 위치에 "X" 삽입 → `"ABXCDE"`
User B (userId=2): 4번 위치에 "Y" 삽입 → `"ABCDY E"`

이 두 편집이 거의 동시에 발생했다고 합시다.

**OT 없이 그냥 적용하면:**

```
A의 화면: "ABCDE" → A가 "X" 삽입 → "ABXCDE"
          → B의 "4번에 Y 삽입" 수신 → "ABXCY DE"  (의도: ABCDE의 4번=D 뒤)
                                       ↑ 문제! X가 끼어들어서 위치가 밀렸는데
                                         B의 인덱스 4를 그대로 적용함
```

**OT를 적용하면:**

```
A의 화면: "ABCDE" → A가 2번에 "X" 삽입 → "ABXCDE"
          → B의 "4번에 Y 삽입" 수신
          → transform: A가 2번에 삽입했으니 B의 4번은 5번으로 보정
          → 5번에 "Y" 삽입 → "ABXCDYE"  ✓ 정확!
```

**transform 규칙 (단순화):**

```
if (상대방의 삽입 위치 <= 나의 삽입 위치) {
    나의 위치를 1 증가시킨다  // 상대가 앞에 글자를 넣었으니 밀림
}
```

**Tie-break (같은 위치에 동시 삽입):**

A와 B가 둘 다 3번 위치에 삽입하면?
→ userId가 작은 쪽(A=1)이 우선 → A의 글자가 앞, B의 글자가 뒤
→ **모든 클라이언트가 같은 규칙을 쓰니까** 최종 결과가 동일

**코드와 연결:**

```javascript
// useEditorSocket.js의 이 부분이 위 로직입니다
const selfHasPriority = currentUserId < broadcast.userId;
let remoteDelta = new Delta(broadcast.ops);
for (let i = 0; i < pending.length; i++) {
    const newRemote = pending[i].transform(remoteDelta, selfHasPriority);
    pending[i] = remoteDelta.transform(pending[i], !selfHasPriority);
    remoteDelta = newRemote;
}
```

- `pending`: 내가 보냈지만 아직 서버에서 echo가 안 온 편집들
- `transform(delta, priority)`: 위치 보정 수행
- `selfHasPriority`: userId가 작은 쪽이 우선

**이렇게 대답하세요:**

> "OT는 동시 편집 시 인덱스 충돌을 보정하는 알고리즘입니다.
> 예를 들어 A가 2번 위치에 글자를 삽입하면,
> B의 편집 위치가 2번 이후라면 1만큼 밀어서 보정합니다.
> 같은 위치에 동시 삽입하면 userId가 작은 쪽을 우선으로 두는 규칙을 쓰고,
> 모든 클라이언트가 동일한 규칙을 적용하기 때문에 최종 문서가 수렴합니다.
> 현재 Phase 1으로 클라이언트에서 transform을 수행하고 있고,
> 서버는 순서 채번과 브로드캐스트만 담당합니다."

---

### Q9. "WebSocket 인증은 HTTP 인증과 뭐가 다른가요?"

**HTTP 인증 (JwtFilter):**

매 요청마다 `Authorization: Bearer {token}` 헤더를 보내고, 매번 검증합니다.

```
GET /api/workspaces  →  JwtFilter 검증  →  Controller
GET /api/feed        →  JwtFilter 검증  →  Controller
POST /api/auth/login →  JwtFilter 통과(permitAll)  →  Controller
```

**WebSocket 인증 (StompAuthChannelInterceptor):**

WebSocket은 **한 번 연결하면 계속 유지**되는 특성이 있습니다.
그래서 **연결(CONNECT) 시점에 1번만 검증**하고, 이후 메시지는 인증 없이 통과합니다.

```
STOMP CONNECT (Authorization: Bearer {token})
    → StompAuthChannelInterceptor 검증 → 연결 수립, Principal 세팅
    → 이후 SEND/SUBSCRIBE 메시지는 Principal이 이미 있으므로 바로 처리
```

**왜 이렇게 설계했나:**

- WebSocket은 연결 유지 비용이 이미 있음 → 매 메시지마다 토큰 검증하면 성능 낭비
- STOMP 프로토콜에서 CONNECT 프레임이 "로그인"에 해당
- 연결 시점에 검증하면 비인증 사용자의 메시지 발행을 근본적으로 차단

**이렇게 대답하세요:**

> "HTTP는 요청-응답 모델이라 매 요청마다 JwtFilter가 토큰을 검증합니다.
> WebSocket은 연결이 유지되는 특성이 있어서,
> STOMP CONNECT 시점에 StompAuthChannelInterceptor가 1번 검증하고
> Principal을 세팅합니다. 이후 메시지는 이미 인증된 연결 위에서 전송되므로
> 매번 토큰 검증을 하지 않아서 실시간 편집 성능에 영향을 주지 않습니다."

---

### Q10. "MongoDB와 MariaDB를 왜 같이 쓰나요? 하나로 통일하면 안 되나요?"

**데이터 특성에 따라 나눴습니다:**

| 데이터 | 저장소 | 이유 |
|--------|--------|------|
| 사용자(User) | MariaDB | 정형 데이터, 이메일 unique 제약, 다른 테이블과 FK 관계 |
| 워크스페이스 | MariaDB | 멤버 관계(N:M)가 있어서 관계형이 적합 |
| 뉴스 기사(Article) | MongoDB | 뉴스마다 필드 구조가 다를 수 있음 (네이버 vs 긱뉴스) |
| DeltaLog | MongoDB | Quill Delta ops가 **JSON 배열** → RDBMS에 넣으면 TEXT에 serialize해야 해서 비효율적 |
| DraftSnapshot | MongoDB | 에디터 내용이 JSON 구조 → MongoDB가 자연스러움 |
| DraftVersion | MongoDB | 위와 동일 |
| Refresh Token | Redis | 임시 데이터 + TTL 자동 만료 |

**핵심 기준:**

- **관계(JOIN)가 필요한 데이터** → MariaDB (사용자 ↔ 워크스페이스 ↔ 멤버)
- **JSON 구조의 비정형 데이터** → MongoDB (Delta ops, 에디터 내용)
- **임시 + 빠른 조회** → Redis (토큰, 시퀀스 번호)

**이렇게 대답하세요:**

> "데이터 특성에 따라 저장소를 나눴습니다.
> 사용자나 워크스페이스처럼 관계형 구조가 필요한 데이터는 MariaDB에,
> Quill 에디터의 Delta ops처럼 JSON 배열 형태의 비정형 데이터는 MongoDB에 저장합니다.
> 예를 들어 DeltaLog의 ops를 MariaDB에 넣으려면 JSON을 TEXT로 직렬화해야 해서
> 쿼리나 인덱싱이 비효율적인데, MongoDB는 JSON을 네이티브로 다루기 때문입니다."

---

## 여기까지 읽었으면 충분합니다

위 10개 질문은 "JWT + Spring Security + 실시간 협업"을 발표할 때
교수님/평가자가 물어볼 수 있는 **가장 가능성 높은 질문들**입니다.

### 대답하는 팁

1. **"~입니다" 로 끝내지 말고, "그래서 ~했습니다"로 끝내세요.**
   - ✗ "JWT는 토큰 기반 인증입니다."
   - ✓ "JWT는 토큰 기반 인증이라서, 서버를 Stateless로 유지할 수 있어 저희 실시간 협업 서비스에 적합해서 선택했습니다."

2. **모르는 질문이 오면, 아는 부분과 연결하세요.**
   - "그 부분은 아직 깊이 다루지 못했는데, 관련해서 저희가 적용한 것은 ~입니다."

3. **코드를 외우지 마세요. 흐름을 기억하세요.**
   - "요청이 들어오면 → JwtFilter가 헤더에서 토큰 추출 → 서명 검증 → SecurityContext에 세팅"
   - 이 **흐름**만 기억하면 코드 없이도 설명할 수 있습니다.
