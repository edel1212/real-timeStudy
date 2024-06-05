package com.yoo.redisSSE.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SseEmitterRepository {

    // thread-safe한 자료구조를 사용한다.
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private Long timeout = 60L * 1000 * 60;

    public SseEmitter save(String eventId) {
        SseEmitter sseEmitter =  new SseEmitter(timeout);
        emitters.put(eventId, sseEmitter);
        return sseEmitter;
    }

    public Optional<SseEmitter> findById(String memberId) {
        return Optional.ofNullable(emitters.get(memberId));
    }

    public void deleteById(String eventId) {
        emitters.remove(eventId);
    }
}
