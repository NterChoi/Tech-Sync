package com.techsync.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "WORKSPACE")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "WORKSPACE_ID")
    private Long workspaceId;

    @Column(name = "WORKSPACE_NAME", nullable = false, length = 100)
    private String workspaceName;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Workspace(String workspaceName, Long ownerId) {
        this.workspaceName = workspaceName;
        this.ownerId = ownerId;
    }

    public void updateName(String workspaceName) {
        this.workspaceName = workspaceName;
    }
}
