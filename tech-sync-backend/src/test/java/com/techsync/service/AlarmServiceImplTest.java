package com.techsync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.techsync.domain.Alarm;
import com.techsync.exception.BusinessException;
import com.techsync.repository.AlarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlarmServiceImplTest {

    @Mock
    private AlarmRepository alarmRepository;

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private AlarmServiceImpl alarmService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        alarmService = new AlarmServiceImpl(
                alarmRepository, sseEmitterRepository, stringRedisTemplate, objectMapper);
    }

    private Alarm savedAlarm(String id, Long userId, boolean read) {
        Alarm alarm = Alarm.builder()
                .userId(userId)
                .type("WORKSPACE_INVITE")
                .message("초대 메시지")
                .workspaceId(7L)
                .read(read)
                .createdAt(LocalDateTime.now())
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(alarm, "id", id);
        return alarm;
    }

    @Test
    @DisplayName("정상: notify 시 알림을 저장하고 Redis 채널로 발행한다")
    void notify_savesAndPublishes() {
        // given
        given(alarmRepository.save(any(Alarm.class))).willReturn(savedAlarm("a1", 2L, false));

        // when
        alarmService.notify(2L, "WORKSPACE_INVITE", "초대 메시지", 7L);

        // then
        verify(alarmRepository).save(any(Alarm.class));
        verify(stringRedisTemplate).convertAndSend(eq(AlarmServiceImpl.ALARM_CHANNEL), anyString());
    }

    @Test
    @DisplayName("정상: subscribe 는 emitter 를 생성/등록하고 반환한다")
    void subscribe_registersEmitter() {
        // when
        SseEmitter emitter = alarmService.subscribe(1L);

        // then
        assertThat(emitter).isNotNull();
        verify(sseEmitterRepository).add(eq(1L), any(SseEmitter.class));
    }

    @Test
    @DisplayName("실패: 다른 사용자의 알림은 읽음 처리할 수 없다")
    void markRead_otherUsersAlarm_throws() {
        // given — 알림 소유자는 userId=2, 요청자는 userId=1
        given(alarmRepository.findById("a1")).willReturn(Optional.of(savedAlarm("a1", 2L, false)));

        // when & then
        assertThatThrownBy(() -> alarmService.markRead(1L, "a1"))
                .isInstanceOf(BusinessException.class);
        verify(alarmRepository, never()).save(any(Alarm.class));
    }
}
