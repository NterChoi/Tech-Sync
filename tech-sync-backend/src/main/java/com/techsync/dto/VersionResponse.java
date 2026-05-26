package com.techsync.dto;

import com.techsync.domain.DraftVersion;
import com.techsync.domain.DraftVersion.VersionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record VersionResponse(
        String id,
        Long workspaceId,
        Long versionNo,
        VersionType versionType,
        List<Map<String, Object>> content,
        Long createdBy,
        LocalDateTime createdAt
) {
    public static VersionResponse from(DraftVersion v) {
        return new VersionResponse(
                v.getId(),
                v.getWorkspaceId(),
                v.getVersionNo(),
                v.getVersionType(),
                v.getContent(),
                v.getCreatedBy(),
                v.getCreatedAt()
        );
    }

    public static VersionResponse summary(DraftVersion v) {
        return new VersionResponse(
                v.getId(),
                v.getWorkspaceId(),
                v.getVersionNo(),
                v.getVersionType(),
                null,
                v.getCreatedBy(),
                v.getCreatedAt()
        );
    }
}
