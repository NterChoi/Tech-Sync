package com.techsync.service;

import com.techsync.domain.DeltaLog;
import com.techsync.dto.DeltaBroadcast;
import com.techsync.dto.DeltaMessage;
import com.techsync.exception.BusinessException;
import com.techsync.repository.DeltaLogRepository;
import com.techsync.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EditorServiceImpl implements EditorService {

    private static final String SEQ_KEY_PREFIX = "delta:seq:";
    private static final String EDIT_TOPIC_PREFIX = "/topic/workspace/";
    private static final String EDIT_TOPIC_SUFFIX = "/edit";

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DeltaLogRepository deltaLogRepository;
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

        DeltaBroadcast broadcast = DeltaBroadcast.of(log);
        messagingTemplate.convertAndSend(
                EDIT_TOPIC_PREFIX + workspaceId + EDIT_TOPIC_SUFFIX,
                broadcast);

        return broadcast;
    }
}
