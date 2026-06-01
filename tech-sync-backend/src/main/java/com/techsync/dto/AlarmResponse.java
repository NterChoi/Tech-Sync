package com.techsync.dto;

import com.techsync.domain.Alarm;

import java.time.LocalDateTime;

public record AlarmResponse(
        String id,
        String type,
        String message,
        Long workspaceId,
        boolean read,
        LocalDateTime createdAt
) {
    public static AlarmResponse from(Alarm alarm) {
        return new AlarmResponse(
                alarm.getId(),
                alarm.getType(),
                alarm.getMessage(),
                alarm.getWorkspaceId(),
                alarm.isRead(),
                alarm.getCreatedAt()
        );
    }
}
