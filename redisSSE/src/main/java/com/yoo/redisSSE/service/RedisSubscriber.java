package com.yoo.redisSSE.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoo.redisSSE.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Log4j2
@RequiredArgsConstructor
@Component
public class RedisSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SseEmitterService sseEmitterService;
    private final String CHANNEL_PREFIX = "emmit_";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel())
                    .substring(CHANNEL_PREFIX.length());

            log.info("channel ::: " + channel);

            NotificationDto notificationDto = objectMapper.readValue(message.getBody(),
                    NotificationDto.class);

            // 클라이언트에게 event 데이터 전송
            sseEmitterService.sendNotificationToClient(channel, notificationDto);
        } catch (IOException e) {
            log.error("IOException is occurred. ", e);
        }
    }
}
