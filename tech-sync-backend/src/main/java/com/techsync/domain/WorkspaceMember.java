package com.techsync.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "WORKSPACE_MEMBER",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_WORKSPACE_USER",
                columnNames = {"WORKSPACE_ID", "USER_ID"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MEMBER_ID")
    private Long memberId;

    @Column(name = "WORKSPACE_ID", nullable = false)
    private Long workspaceId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ROLE", nullable = false, length = 20)
    private String role;

    @CreatedDate
    @Column(name = "JOINED_AT", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Builder
    public WorkspaceMember(Long workspaceId, Long userId, String role) {
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.role = role;
    }
}
