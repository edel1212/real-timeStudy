package com.yoo.simple.WebSocket.controller;

import com.yoo.simple.WebSocket.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import java.util.Map;

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
    //@SendTo("구독 주소") // ℹ️ 해당 메서드를 타면 지정 구독자들에게 같은 메세지가 전달 가능하다.
    public void message(ChatMessage message,
                        @Header("Authorization") String authHeader,
                        @Headers Map<String, Object> headers) {
        // 개별 헤더 값 출력
        log.info("------------");
        log.info("Authorization Header ::: {}", authHeader);
        log.info("message ::: {}", message);
        log.info("------------");

        // 전체 헤더 출력
        log.info("Headers ::: {}", headers);

        // 메시지 처리 로직
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }
}
