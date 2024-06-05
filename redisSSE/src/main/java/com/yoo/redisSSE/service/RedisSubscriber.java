package com.yoo.redisSSE.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoo.redisSSE.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${redis.ssePrefix}")
    private String channelPrefix;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel())
                    .substring(channelPrefix.length());

            log.info("channel ::: " + channel);
            log.info("message ::: " + message.getBody().toString());

            NotificationDto notificationDto = objectMapper.readValue(message.getBody(),
                    NotificationDto.class);
            // 클라이언트에게 event 데이터 전송
            sseEmitterService.sendNotificationToClient(notificationDto);
        } catch (IOException e) {
            log.error("IOException is occurred. ", e);
        }
    }
}
