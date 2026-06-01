package com.techsync.service;

import com.techsync.dto.AlarmEvent;
import com.techsync.dto.AlarmResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AlarmService {

    /** SSE 구독 — 호출 사용자의 emitter 를 생성/등록하고 반환한다. */
    SseEmitter subscribe(Long userId);

    /** 알림 생성 — MongoDB 저장 후 Redis 채널로 발행한다. */
    void notify(Long targetUserId, String type, String message, Long workspaceId);

    /** Redis 구독자가 호출 — 이 인스턴스의 로컬 emitter 들에 이벤트를 전달한다. */
    void dispatchToLocalEmitters(AlarmEvent event);

    List<AlarmResponse> getMyAlarms(Long userId);

    long getUnreadCount(Long userId);

    void markRead(Long userId, String alarmId);
}
