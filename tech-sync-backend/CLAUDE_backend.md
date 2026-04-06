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

## API 응답 형식
모든 REST 응답은 아래 공통 래퍼를 사용한다.
```java
// 성공
ApiResponse.success(data)         // 200
ApiResponse.created(data)         // 201

// 실패
ApiResponse.error(code, message)  // 400·404·500
```

## WebSocket 경로 규칙
```
/app/edit/{roomId}       → 클라이언트 → 서버 (편집 Delta 전송)
/app/chat/{roomId}       → 클라이언트 → 서버 (채팅 메시지)
/topic/room/{roomId}     → 서버 → 전체 구독자 브로드캐스트
/queue/user/{userId}     → 서버 → 특정 사용자 1:1
```

## 보안 체크리스트
코드 작성 시 아래 항목을 반드시 확인한다.
- [ ] API Key, DB 비밀번호가 코드에 하드코딩되어 있지 않은가
- [ ] `@PreAuthorize` 또는 Security Filter로 인증이 적용되어 있는가
- [ ] SQL/NoSQL 인젝션 가능성이 없는가 (JPA 파라미터 바인딩 사용)
- [ ] 민감 정보가 로그에 출력되지 않는가
