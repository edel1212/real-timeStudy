package com.yoo.simple.WebSocket.controller;

import com.yoo.simple.WebSocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Log4j2
@RequiredArgsConstructor
@Controller
public class ChatController {
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * ✅ MessageMapping 어노테이션을 사용한다.
     * - 입장, 글쓰기 모두 이곳을 통해 전달 된다.
     * */
    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        log.info("------------");
        log.info("message ::: {}",message);
        log.info("------------");
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }
}
