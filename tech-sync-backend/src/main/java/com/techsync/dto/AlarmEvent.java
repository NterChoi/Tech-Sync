package com.techsync.dto;

/**
 * Redis Pub/Sub 채널로 발행되는 알림 이벤트 페이로드.
 * 어느 인스턴스에서 발행되든 모든 구독 인스턴스가 받아,
 * targetUserId 의 로컬 SSE emitter 로 전달한다.
 */
public record AlarmEvent(
        Long targetUserId,
        AlarmResponse alarm
) {
}
