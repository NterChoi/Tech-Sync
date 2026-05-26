package com.techsync.service;

import com.techsync.domain.DeltaLog;
import com.techsync.domain.DraftSnapshot;
import com.techsync.dto.CursorBroadcast;
import com.techsync.dto.CursorMessage;
import com.techsync.dto.DeltaBroadcast;
import com.techsync.dto.DeltaMessage;
import com.techsync.dto.SnapshotResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.DeltaLogRepository;
import com.techsync.repository.DraftSnapshotRepository;
import com.techsync.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EditorServiceImpl implements EditorService {

    private static final String SEQ_KEY_PREFIX = "delta:seq:";
    private static final String EDIT_TOPIC_PREFIX = "/topic/workspace/";
    private static final String EDIT_TOPIC_SUFFIX = "/edit";
    private static final String CURSOR_TOPIC_SUFFIX = "/cursor";

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DeltaLogRepository deltaLogRepository;
    private final DraftSnapshotRepository draftSnapshotRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public DeltaBroadcast applyDelta(Long workspaceId, Long userId, DeltaMessage message) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new BusinessException("워크스페이스 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
        }

        Long nextSeqNo = redisTemplate.opsForValue().increment(SEQ_KEY_PREFIX + workspaceId);
        if (nextSeqNo == null) {
            throw new BusinessException("seqNo 채번에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        DeltaLog log = deltaLogRepository.save(DeltaLog.builder()
                .workspaceId(workspaceId)
                .seqNo(nextSeqNo)
                .userId(userId)
                .ops(message.ops())
                .createdAt(LocalDateTime.now())
                .build());

        DeltaBroadcast broadcast = DeltaBroadcast.of(log, message.clientSeqNo());
        messagingTemplate.convertAndSend(
                EDIT_TOPIC_PREFIX + workspaceId + EDIT_TOPIC_SUFFIX,
                broadcast);

        return broadcast;
    }

    @Override
    public CursorBroadcast broadcastCursor(Long workspaceId, Long userId, CursorMessage message) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new BusinessException("워크스페이스 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
        }

        CursorBroadcast broadcast = CursorBroadcast.of(workspaceId, userId, message);
        messagingTemplate.convertAndSend(
                EDIT_TOPIC_PREFIX + workspaceId + CURSOR_TOPIC_SUFFIX,
                broadcast);

        return broadcast;
    }

    @Override
    public SnapshotResponse saveSnapshot(Long workspaceId, Long userId,
                                         List<Map<String, Object>> content) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new BusinessException("워크스페이스 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
        }

        DraftSnapshot snapshot = draftSnapshotRepository.findByWorkspaceId(workspaceId)
                .map(existing -> {
                    existing.updateContent(content, userId);
                    return existing;
                })
                .orElse(DraftSnapshot.builder()
                        .workspaceId(workspaceId)
                        .content(content)
                        .lastEditorId(userId)
                        .updatedAt(LocalDateTime.now())
                        .build());

        return SnapshotResponse.from(draftSnapshotRepository.save(snapshot));
    }

    @Override
    public SnapshotResponse getSnapshot(Long workspaceId, Long userId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new BusinessException("워크스페이스 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
        }

        return draftSnapshotRepository.findByWorkspaceId(workspaceId)
                .map(SnapshotResponse::from)
                .orElse(null);
    }
}
