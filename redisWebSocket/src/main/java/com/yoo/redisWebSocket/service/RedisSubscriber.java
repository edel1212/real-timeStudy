package com.yoo.redisWebSocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoo.redisWebSocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Log4j2
@RequiredArgsConstructor
@Component
public class RedisSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    // ℹ️ STOMP 주입 (메세지를 전송하기 위한 의존성 주입)
    private final SimpMessageSendingOperations messagingTemplate;
    @Value("${redis.ssePrefix}")
    private String channelPrefix;

    /**
     * 구독자들에게 <pre>convertAndSend() 시</pre> 사용 될 메서드이다
     * */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // 1 . Redis에서만 사용했던 prefix 제거
            String roomId = new String(message.getChannel())
                    .substring(channelPrefix.length());

            log.info("channel ::: {}  ",roomId);
            
            // 2 . RedisValue -> JavaObject 변환
            ChatMessage chatMessage = objectMapper.readValue(message.getBody(),
                    ChatMessage.class);

            // 3 . 구독자들에게 Stomp를 사용해서 메세지 전송
            messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, chatMessage);

        } catch (IOException e) {
            log.error("IOException is occurred. ", e);
        } // try - catch
    }
}
