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

    public SseEmitter createSseEmitter(String channel){
        return sseEmitterRepository.save(channel);
    }

    public void removeChannel(String channel){
        sseEmitterRepository.deleteById(channel);
    }

    public void sendNotificationToClient(NotificationDto notificationDto) {
        String accountId = notificationDto.getChannel();
        sseEmitterRepository.findById(accountId)
                .ifPresent(emitter -> sendMessage(notificationDto, emitter));
    }

    public void sendMessage(NotificationDto data, SseEmitter sseEmitter) {
        log.info("send to client :[{}]",  data);
        String channel =  data.getChannel();
        try {
            sseEmitter.send(SseEmitter.event()
                    .id(channel)
                    .name("sse")
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            log.error("IOException | IllegalStateException is occurred. ", e);
            // 에러가 발생할 경우 채널 삭제
            sseEmitterRepository.deleteById(channel);
        } // try - catch
    }
}
