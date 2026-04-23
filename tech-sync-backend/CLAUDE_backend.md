# Backend 규칙 (tech-sync-backend)

## 패키지 네이밍
```
com.techsync.controller   → *Controller.java
com.techsync.service      → *Service.java (인터페이스) + *ServiceImpl.java
com.techsync.repository   → *Repository.java (JPA/MongoRepository 상속)
com.techsync.domain       → Entity, Document 클래스
com.techsync.dto          → *Request.java, *Response.java
com.techsync.config       → *Config.java
com.techsync.exception    → *Exception.java, GlobalExceptionHandler.java
```

## Entity 작성 규칙
- MariaDB Entity: `@Entity`, `@Table(name="...")`, Lombok `@Getter` `@NoArgsConstructor`
- MongoDB Document: `@Document(collection="...")`, `@Id`는 String 타입
- 생성일/수정일: `@CreatedDate`, `@LastModifiedDate` 사용 (직접 set 금지)
    - 이유: JPA Auditing이 일관된 시간 기록을 보장하며, 수동 set은 테스트 시 시간 불일치를 유발한다

### Entity 작성 예시 (이 패턴을 따를 것)
```java
@Entity
@Table(name = "TABLE_NAME")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class ExampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EXAMPLE_ID")
    private Long exampleId;

    @Column(name = "EXAMPLE_NAME", nullable = false, length = 100)
    private String exampleName;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public ExampleEntity(String exampleName) {
        this.exampleName = exampleName;
    }
}
```

## API 응답 형식
모든 REST 응답은 아래 공통 래퍼를 사용한다.
```java
// 성공
ApiResponse.success(data)         // 200
ApiResponse.created(data)         // 201

// 실패
ApiResponse.error(message)        // BusinessException에서 상태코드 결정
```

### Controller 메서드 작성 예시
```java
@PostMapping
public ResponseEntity<ApiResponse<ExampleResponse>> create(
        @Valid @RequestBody ExampleRequest request) {
    ExampleResponse response = exampleService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(response));
}

@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ExampleResponse>> getById(@PathVariable Long id) {
    ExampleResponse response = exampleService.getById(id);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

### 에러 처리 예시
```java
// Service에서 BusinessException 발생 → GlobalExceptionHandler가 처리
throw new BusinessException("리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
throw new BusinessException("이미 존재하는 데이터입니다.", HttpStatus.CONFLICT);
```

## WebSocket 경로 규칙
```
/app/edit/{workspaceId}            → 클라이언트 → 서버 (편집 Delta 전송)
/app/chat/{workspaceId}            → 클라이언트 → 서버 (채팅 메시지)
/topic/workspace/{workspaceId}/edit → 서버 → 전체 구독자 브로드캐스트 (편집)
/topic/workspace/{workspaceId}/chat → 서버 → 전체 구독자 브로드캐스트 (채팅)
/queue/user/{userId}     → 서버 → 특정 사용자 1:1
```

## 보안 체크리스트
> 에이전트는 코드 작성 완료 후 아래 항목을 자동으로 점검한다. 하나라도 위반 시 사용자에게 경고를 출력하고 수정을 제안한다.

- [ ] API Key, DB 비밀번호가 코드에 하드코딩되어 있지 않은가 → 환경변수(`${...}`) 사용 확인
- [ ] `@PreAuthorize` 또는 Security Filter로 인증이 적용되어 있는가 → `/api/auth/**` 외 모든 경로
- [ ] SQL/NoSQL 인젝션 가능성이 없는가 → JPA 파라미터 바인딩, MongoTemplate에서 문자열 결합 금지
- [ ] 민감 정보가 로그에 출력되지 않는가 → password, token, secret 필드가 log에 포함되지 않을 것

## 테스트 작성 규칙
새 Service를 작성하면 반드시 대응하는 테스트를 함께 작성한다.

### 테스트 구조 예시
```java
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ExampleServiceTest {

    @Autowired
    private ExampleService exampleService;

    @Test
    @DisplayName("정상 케이스: 설명")
    void success_case() {
        // given
        // when
        // then
    }

    @Test
    @DisplayName("실패 케이스: 설명")
    void failure_case() {
        // given
        // when & then
        assertThrows(BusinessException.class, () -> {
            exampleService.someMethod(invalidInput);
        });
    }
}
```

### 테스트 최소 기준
| 대상 | 최소 테스트 수 | 필수 케이스 |
|------|---------------|------------|
| Service | 2개 이상 | 정상 1 + 실패/예외 1 |
| Controller | 선택 | MockMvc 통합 테스트 (인증 포함) |
| Repository | 커스텀 쿼리가 있을 때만 | 쿼리 결과 검증 |