package com.yoo.redisSSE.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final Map<String, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String username) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        userEmitters.put(username, emitter);

        emitter.onCompletion(() -> userEmitters.remove(username));
        emitter.onTimeout(() -> userEmitters.remove(username));
        emitter.onError((e) -> userEmitters.remove(username));

        return emitter;
    }

    public void sendNotification(String username, String message) {
        sendRealTimeNotification(username, message);
    }

    private void sendRealTimeNotification(String username, String message) {
        SseEmitter emitter = userEmitters.get(username);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message));
            } catch (Exception e) {
                // 예외 처리
            }
        }
    }


}
