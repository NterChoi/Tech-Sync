package com.techsync.dto;

import com.techsync.domain.DraftSnapshot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record SnapshotResponse(
        Long workspaceId,
        List<Map<String, Object>> content,
        Long lastEditorId,
        LocalDateTime updatedAt
) {
    public static SnapshotResponse from(DraftSnapshot snapshot) {
        return new SnapshotResponse(
                snapshot.getWorkspaceId(),
                snapshot.getContent(),
                snapshot.getLastEditorId(),
                snapshot.getUpdatedAt()
        );
    }
}
