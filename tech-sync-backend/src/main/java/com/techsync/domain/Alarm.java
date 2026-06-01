package com.techsync.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "ALARM")
@Getter
@NoArgsConstructor
public class Alarm {

    @Id
    private String id;

    /** 알림을 받는 대상 사용자 ID */
    @Indexed
    @Field("USER_ID")
    private Long userId;

    /** 알림 종류 (예: WORKSPACE_INVITE) */
    @Field("TYPE")
    private String type;

    /** 사용자에게 표시할 메시지 */
    @Field("MESSAGE")
    private String message;

    /** 클릭 시 이동할 워크스페이스 ID (없으면 null) */
    @Field("WORKSPACE_ID")
    private Long workspaceId;

    @Field("READ")
    private boolean read;

    @Field("CREATED_AT")
    private LocalDateTime createdAt;

    @Builder
    public Alarm(Long userId, String type, String message, Long workspaceId,
                 boolean read, LocalDateTime createdAt) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.workspaceId = workspaceId;
        this.read = read;
        this.createdAt = createdAt;
    }

    public void markRead() {
        this.read = true;
    }
}
