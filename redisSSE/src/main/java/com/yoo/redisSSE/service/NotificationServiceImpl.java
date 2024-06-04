package com.yoo.redisSSE.service;

import com.yoo.redisSSE.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@Log4j2
@RequiredArgsConstructor
public class NotificationServiceImpl{
    private final SseEmitterService sseEmitterService;
    private final RedisMessageService redisMessageService;

    // ℹ️ 구독
    public SseEmitter subscribe(String memberKey) {
        SseEmitter sseEmitter = sseEmitterService.createEmitter(memberKey);

        sseEmitterService.send(NotificationDto.builder().message("Hi!!").build(), memberKey, sseEmitter); // send dummy

        redisMessageService.subscribe(memberKey); // redis 구독

        sseEmitter.onTimeout(sseEmitter::complete);
        sseEmitter.onError((e) -> sseEmitter.complete());
        sseEmitter.onCompletion(() -> {
            sseEmitterService.deleteEmitter(memberKey);
            redisMessageService.removeSubscribe(memberKey); // 구독한 채널 삭제
        });
        return sseEmitter;
    }


    public void sendNotification(String accountId, String message) {
        // redis 이벤트 발행
        redisMessageService.publish(accountId, NotificationDto.builder().message(message).build());
    }

    


}