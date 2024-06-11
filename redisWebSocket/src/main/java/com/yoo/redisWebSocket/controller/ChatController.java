package com.yoo.redisWebSocket.controller;

import com.yoo.redisWebSocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Log4j2
@RequiredArgsConstructor
@Controller
public class ChatController {
    private final SimpMessageSendingOperations messagingTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.ssePrefix}")
    private String channelPrefix;

    @MessageMapping("/chat/message")
    //@SendTo("구독 주소") // ℹ️ 해당 메서드를 타면 지정 구독자들에게 같은 메세지가 전달 가능하다.
    public void message(ChatMessage message,
                        @Header("Authorization") String authHeader,
                        @Headers Map<String, Object> headers) {

        // Pub
        redisTemplate.convertAndSend(getChannelName(message.getRoomId()), message);
    }

    private String getChannelName(String id) {
        return channelPrefix + id;
    }
}
