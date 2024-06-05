package com.yoo.redisSSE.service;

import com.yoo.redisSSE.dto.NotificationDto;
import com.yoo.redisSSE.repository.SseEmitterRepository;
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
    private final SseEmitterRepository sseEmitterRepository;
    private Long timeout = 60L * 1000 * 60;
    // ℹ️ 구독
    public SseEmitter subscribe(String accountId) {
        SseEmitter sseEmitter = sseEmitterRepository.save(accountId, new SseEmitter(timeout));

        sseEmitterService.send(NotificationDto.builder().message("Hi!!").build(), accountId, sseEmitter); // send dummy

        redisMessageService.subscribe(accountId); // redis 구독

        sseEmitter.onTimeout(sseEmitter::complete);
        sseEmitter.onError((e) -> sseEmitter.complete());
        sseEmitter.onCompletion(() -> {
            sseEmitterService.deleteEmitter(accountId);
            redisMessageService.removeSubscribe(accountId); // 구독한 채널 삭제
        });
        return sseEmitter;
    }


    public void sendNotification(String accountId, String message) {
        // redis 이벤트 발행
        redisMessageService.publish(accountId, NotificationDto.builder().message(message).build());
    }

    


}