package com.techsync.dto;

import com.techsync.domain.Workspace;

import java.time.LocalDateTime;
import java.util.List;

public record WorkspaceDetailResponse(
        Long workspaceId,
        String workspaceName,
        Long ownerId,
        String ownerName,
        LocalDateTime createdAt,
        List<MemberResponse> members
) {
    public static WorkspaceDetailResponse of(Workspace workspace, String ownerName,
                                              List<MemberResponse> members) {
        return new WorkspaceDetailResponse(
                workspace.getWorkspaceId(),
                workspace.getWorkspaceName(),
                workspace.getOwnerId(),
                ownerName,
                workspace.getCreatedAt(),
                members
        );
    }
}
