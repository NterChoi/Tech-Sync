package com.techsync.service;

import com.techsync.domain.DeltaLog;
import com.techsync.dto.DeltaBroadcast;
import com.techsync.dto.DeltaMessage;
import com.techsync.exception.BusinessException;
import com.techsync.repository.DeltaLogRepository;
import com.techsync.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EditorServiceImplTest {

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private DeltaLogRepository deltaLogRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EditorServiceImpl editorService;

    private DeltaMessage message;

    @BeforeEach
    void setUp() {
        message = new DeltaMessage(
                List.of(Map.of("retain", 5), Map.of("insert", "Hello")),
                10L);
    }

    private DeltaLog savedLog(Long workspaceId, Long seqNo, Long userId,
                              List<Map<String, Object>> ops) {
        DeltaLog log = DeltaLog.builder()
                .workspaceId(workspaceId)
                .seqNo(seqNo)
                .userId(userId)
                .ops(ops)
                .createdAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(log, "id", "mongo-id-" + seqNo);
        return log;
    }

    // ==================== 정상 ====================

    @Test
    @DisplayName("applyDelta: 멤버가 Delta 전송 시 seqNo 채번 + DELTA_LOG 저장 + 브로드캐스트")
    void applyDelta_success() {
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 100L)).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("delta:seq:1")).willReturn(11L);
        given(deltaLogRepository.save(any(DeltaLog.class)))
                .willAnswer(inv -> {
                    DeltaLog arg = inv.getArgument(0);
                    ReflectionTestUtils.setField(arg, "id", "mongo-id-11");
                    return arg;
                });

        DeltaBroadcast result = editorService.applyDelta(1L, 100L, message);

        assertThat(result.workspaceId()).isEqualTo(1L);
        assertThat(result.seqNo()).isEqualTo(11L);
        assertThat(result.userId()).isEqualTo(100L);
        assertThat(result.ops()).hasSize(2);

        ArgumentCaptor<DeltaLog> logCaptor = ArgumentCaptor.forClass(DeltaLog.class);
        verify(deltaLogRepository).save(logCaptor.capture());
        DeltaLog saved = logCaptor.getValue();
        assertThat(saved.getWorkspaceId()).isEqualTo(1L);
        assertThat(saved.getSeqNo()).isEqualTo(11L);
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getOps()).isEqualTo(message.ops());

        verify(messagingTemplate).convertAndSend(
                eq("/topic/workspace/1/edit"),
                any(DeltaBroadcast.class));
    }

    @Test
    @DisplayName("applyDelta: 같은 워크스페이스에서 두 번 호출 시 Redis INCR이 두 번 호출된다")
    void applyDelta_seqNoSequential() {
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 100L)).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("delta:seq:1")).willReturn(11L, 12L);
        given(deltaLogRepository.save(any(DeltaLog.class)))
                .willAnswer(inv -> inv.getArgument(0));

        DeltaBroadcast first = editorService.applyDelta(1L, 100L, message);
        DeltaBroadcast second = editorService.applyDelta(1L, 100L, message);

        assertThat(first.seqNo()).isEqualTo(11L);
        assertThat(second.seqNo()).isEqualTo(12L);
        verify(valueOperations, org.mockito.Mockito.times(2))
                .increment("delta:seq:1");
    }

    // ==================== 실패 ====================

    @Test
    @DisplayName("applyDelta: 워크스페이스 멤버가 아니면 403 FORBIDDEN, 저장/브로드캐스트 발생하지 않음")
    void applyDelta_notMember_forbidden() {
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 100L)).willReturn(false);

        assertThatThrownBy(() -> editorService.applyDelta(1L, 100L, message))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        verify(deltaLogRepository, never()).save(any(DeltaLog.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }

    @Test
    @DisplayName("applyDelta: Redis INCR이 null을 반환하면 500 INTERNAL_SERVER_ERROR")
    void applyDelta_redisReturnsNull_internalError() {
        given(workspaceMemberRepository.existsByWorkspaceIdAndUserId(1L, 100L)).willReturn(true);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("delta:seq:1")).willReturn(null);

        assertThatThrownBy(() -> editorService.applyDelta(1L, 100L, message))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        verify(deltaLogRepository, never()).save(any(DeltaLog.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }
}
