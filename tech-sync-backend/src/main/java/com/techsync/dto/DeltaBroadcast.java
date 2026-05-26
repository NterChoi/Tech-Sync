package com.techsync.dto;

import com.techsync.domain.DeltaLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DeltaBroadcast(
        Long workspaceId,
        Long seqNo,
        Long userId,
        List<Map<String, Object>> ops,
        LocalDateTime timestamp,
        Long clientSeqNo
) {
    public static DeltaBroadcast of(DeltaLog log, Long clientSeqNo) {
        return new DeltaBroadcast(
                log.getWorkspaceId(),
                log.getSeqNo(),
                log.getUserId(),
                log.getOps(),
                log.getCreatedAt(),
                clientSeqNo
        );
    }
}
