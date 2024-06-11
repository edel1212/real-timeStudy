package com.yoo.redisWebSocket.controller;

import com.yoo.redisWebSocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Log4j2
@RequiredArgsConstructor
@Controller
public class ChatController {
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.ssePrefix}")
    private String channelPrefix;

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        // 1. RoomId를 가져옴
        String roomId = message.getRoomId();
        // 2 . roomId -> Redis 식별키 변경
        String topic = channelPrefix + roomId;
        // 3 . 구독자(topic)들에게 emmit
        redisTemplate.convertAndSend(topic , message);
    }

}
