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
    public SseEmitter subscribe(String accountId) {
        // 1 . SSE 객체 생성
        SseEmitter sseEmitter = sseEmitterService.createSseEmitter(accountId);

        // 2 . 메세지 전송 최초 1회 필수
        NotificationDto data = NotificationDto.builder()
                .channel(accountId)
                .message("Create SSE Owner : " + accountId )
                .build();
        sseEmitterService.sendMessage(data, sseEmitter);

        // 3 . Redis 구독
        redisMessageService.subscribe(accountId);
        
        // 4 . SSE 성공 및 실패 처리
        sseEmitter.onTimeout(sseEmitter::complete);
        sseEmitter.onError((e) -> sseEmitter.complete());
        sseEmitter.onCompletion(() -> {
            // Map에 저장된 sseEmitter 삭제
            sseEmitterService.removeChannel(accountId);
            // 구독한 채널 삭제
            redisMessageService.removeSubscribe(accountId);
        });
        return sseEmitter;
    }


    public void sendNotification(String accountId, String message) {
        // redis 이벤트 발행
        redisMessageService.publish(accountId, NotificationDto.builder().channel(accountId).message(message).build());
    }

}