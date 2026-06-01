package com.techsync.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techsync.domain.Alarm;
import com.techsync.dto.AlarmEvent;
import com.techsync.dto.AlarmResponse;
import com.techsync.exception.BusinessException;
import com.techsync.repository.AlarmRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmServiceImpl implements AlarmService {

    private static final Logger log = LoggerFactory.getLogger(AlarmServiceImpl.class);

    /** Redis Pub/Sub 채널명 */
    public static final String ALARM_CHANNEL = "alarm";
    /** SSE 연결 유지 시간 (30분). 만료 시 클라이언트가 재연결한다. */
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final AlarmRepository alarmRepository;
    private final SseEmitterRepository sseEmitterRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        sseEmitterRepository.add(userId, emitter);
        emitter.onCompletion(() -> sseEmitterRepository.remove(userId, emitter));
        emitter.onTimeout(() -> {
            sseEmitterRepository.remove(userId, emitter);
            emitter.complete();
        });
        emitter.onError(e -> sseEmitterRepository.remove(userId, emitter));

        // 연결 직후 더미 이벤트를 보내 프록시 버퍼를 flush 하고 onopen 을 트리거한다.
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            sseEmitterRepository.remove(userId, emitter);
        }

        return emitter;
    }

    @Override
    public void notify(Long targetUserId, String type, String message, Long workspaceId) {
        Alarm alarm = alarmRepository.save(Alarm.builder()
                .userId(targetUserId)
                .type(type)
                .message(message)
                .workspaceId(workspaceId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());

        AlarmEvent event = new AlarmEvent(targetUserId, AlarmResponse.from(alarm));
        try {
            stringRedisTemplate.convertAndSend(ALARM_CHANNEL, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            // 발행 실패해도 알림은 DB 에 남으므로 다음 조회 시 노출된다.
            log.warn("알림 Redis 발행 실패 (userId={}): {}", targetUserId, e.getMessage());
        }
    }

    @Override
    public void dispatchToLocalEmitters(AlarmEvent event) {
        List<SseEmitter> emitters = sseEmitterRepository.get(event.targetUserId());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alarm").data(event.alarm()));
            } catch (IOException e) {
                sseEmitterRepository.remove(event.targetUserId(), emitter);
            }
        }
    }

    @Override
    public List<AlarmResponse> getMyAlarms(Long userId) {
        return alarmRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AlarmResponse::from)
                .toList();
    }

    @Override
    public long getUnreadCount(Long userId) {
        return alarmRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markRead(Long userId, String alarmId) {
        Alarm alarm = alarmRepository.findById(alarmId)
                .orElseThrow(() -> new BusinessException("알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!alarm.getUserId().equals(userId)) {
            throw new BusinessException("본인의 알림만 읽음 처리할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        alarm.markRead();
        alarmRepository.save(alarm);
    }
}
