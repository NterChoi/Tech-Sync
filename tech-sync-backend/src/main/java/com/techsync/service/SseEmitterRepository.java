package com.techsync.service;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 이 서버 인스턴스에 연결된 SSE emitter 를 사용자별로 보관한다.
 * 한 사용자가 여러 탭/기기로 접속할 수 있으므로 userId 당 다중 emitter 를 허용한다.
 */
@Repository
public class SseEmitterRepository {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void add(Long userId, SseEmitter emitter) {
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }
    }

    public List<SseEmitter> get(Long userId) {
        return emitters.getOrDefault(userId, List.of());
    }
}
