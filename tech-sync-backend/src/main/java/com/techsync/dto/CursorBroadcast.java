package com.techsync.dto;

public record CursorBroadcast(
        Long workspaceId,
        Long userId,
        CursorMessage.CursorRange range
) {
    public static CursorBroadcast of(Long workspaceId, Long userId, CursorMessage message) {
        return new CursorBroadcast(workspaceId, userId, message.range());
    }
}
