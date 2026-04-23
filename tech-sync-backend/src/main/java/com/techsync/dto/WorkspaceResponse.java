package com.techsync.dto;

import com.techsync.domain.Workspace;

import java.time.LocalDateTime;

public record WorkspaceResponse(
        Long workspaceId,
        String workspaceName,
        Long ownerId,
        String ownerName,
        LocalDateTime createdAt
) {
    public static WorkspaceResponse of(Workspace workspace, String ownerName) {
        return new WorkspaceResponse(
                workspace.getWorkspaceId(),
                workspace.getWorkspaceName(),
                workspace.getOwnerId(),
                ownerName,
                workspace.getCreatedAt()
        );
    }
}
