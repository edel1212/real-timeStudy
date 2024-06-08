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
            // Redis에서 저장된 Key(채널)값은 Prefix를 달아 저장했기에
            // 해당 Prefix를 제거한 후 Map에 저장된 SS
            String channel = new String(message.getChannel())
                    .substring(channelPrefix.length());

            log.info("channel ::: " + channel);
            log.info("message ::: " + message.getBody().toString());

            NotificationDto notificationDto = objectMapper.readValue(message.getBody(),
                    NotificationDto.class);
            // 구독하고 있는 Client들에게 메세지를 전달한다.
            sseEmitterService.sendNotificationToClient(notificationDto);
        } catch (IOException e) {
            log.error("IOException is occurred. ", e);
        }
    }
}
