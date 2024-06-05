package com.yoo.redisSSE.service;

import com.yoo.redisSSE.dto.NotificationDto;
import com.yoo.redisSSE.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@Log4j2
@RequiredArgsConstructor
public class NotificationServiceImpl{
    private final SseEmitterService sseEmitterService;
    private final RedisMessageService redisMessageService;
    private final SseEmitterRepository sseEmitterRepository;

    // ℹ️ 구독
    public SseEmitter subscribe(String accountId) {
        // 1 . SSE 객체 생성
        SseEmitter sseEmitter = sseEmitterRepository.save(accountId);

        // 2 . 메세지 전송 최초 1회 필수
        NotificationDto data = NotificationDto.builder()
                .accountId(accountId)
                .message("Create SSE Owner : " + accountId )
                .build();
        this.sendMessage(data, sseEmitter);

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

    private void sendMessage(NotificationDto data, SseEmitter sseEmitter) {
        log.info("send to client :[{}]", data);
        String accountId = data.getAccountId();
        try {
            sseEmitter.send(SseEmitter.event()
                    .id(accountId)
                    .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            log.error("IOException | IllegalStateException is occurred. ", e);
            sseEmitterRepository.deleteById(accountId);
        }// try - catch
    }


}