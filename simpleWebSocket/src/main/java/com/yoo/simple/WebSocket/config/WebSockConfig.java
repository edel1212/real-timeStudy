package com.yoo.simple.WebSocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
//  웹소켓 활성화
@EnableWebSocketMessageBroker
public class WebSockConfig implements WebSocketMessageBrokerConfigurer {
    /**
     * WebSocket 메시징을 설정할 때 호출됩니다. 주로 메시지 브로커의 설정을 담당
     * */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //  메모리 기반 메시지 브로커를 활성화
        // /sub로 시작하는 목적지로 클라이언트가 구독한 경우 이 브로커가 메시지를 처리합니다.
        registry.enableSimpleBroker("/sub");
        // 애플리케이션에서 메시지를 보내기 위한 목적지 경로의 접두사를 설정합니다.
        // 서버는 이 접두사를 보고 해당 메시지를 애플리케이션의 메시지 처리기로 라우팅합니다.
        registry.setApplicationDestinationPrefixes("/pub");
    }

    /**
     * WebSocket STOMP 엔드포인트를 등록하기 위해 사용됩니다.
     * */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 이 URL로 WebSocket 연결을 시작하게 됩니다
        // ws://<서버 주소>/ws-stomp로 WebSocket 연결을 시도할 수 있습니다.
        registry.addEndpoint("/ws-stomp")
                // CORS(Cross-Origin Resource Sharing) 정책을 설정합니다
                //.setAllowedOrigins("http://localhost:8080", "http://localhost:8081", "http://127.0.0.1:5500")
                .setAllowedOriginPatterns("*")
                // WebSocket을 지원하지 않는 브라우저에서도 STOMP 프로토콜을 사용할 수 있도록 SockJS 폴백(fallback) 옵션을 활성화합니다.
                // 삽질 추가 .. -> javascript 확인 시 아래 옵션을 끄면 Cors 에러가 뜬다 .. 왜지 ..
                .withSockJS()
        ;
    }
}
