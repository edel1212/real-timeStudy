package com.yoo.redisSSE.service;

import com.yoo.redisSSE.dto.NotificationDto;
import com.yoo.redisSSE.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Log4j2
@RequiredArgsConstructor
@Service
public class SseEmitterService {

    private final SseEmitterRepository sseEmitterRepository;

    private Long timeout = 60L * 1000 * 60;

    public SseEmitter createEmitter(String emitterKey) {
        return sseEmitterRepository.save(emitterKey, new SseEmitter(timeout));
    }

    public void deleteEmitter(String emitterKey) {
        sseEmitterRepository.deleteById(emitterKey);
    }

    public void sendNotificationToClient(String emitterKey, NotificationDto notificationDto) {
        sseEmitterRepository.findById(emitterKey)
                .ifPresent(emitter -> send(notificationDto, emitterKey, emitter));
    }

    public void send(Object data, String emitterKey, SseEmitter sseEmitter) {
        try {
            log.info("send to client {}:[{}]", emitterKey, data);
            sseEmitter.send(SseEmitter.event()
                    .id(emitterKey)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            log.error("IOException | IllegalStateException is occurred. ", e);
            sseEmitterRepository.deleteById(emitterKey);
        }
    }
}
