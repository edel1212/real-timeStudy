package com.yoo.redisSSE.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationService {
    // 타임 아웃 시간
    Long DEFAULT_TIMEOUT = 60L * 1000 * 60;

    /**
     * <h3>클라이언트가 구독을 위해 호출하는 메서드.</h3>
     *
     * @param accountId - 구독하는 클라이언트의 사용자 아이디.
     * @return SseEmitter - 서버에서 보낸 이벤트 Emitter
     */
    SseEmitter subscribe(String accountId);

    /**
     * 서버의 이벤트를 클라이언트에게 보내는 메서드
     * 다른 서비스 로직에서 이 메서드를 사용해 데이터를 Object event에 넣고 전송하면 된다.
     *
     * @param accountId - 메세지를 전송할 사용자의 아이디.
     * @param event     - 전송할 이벤트 객체.
     */
    void notify(String accountId, Object event);
}
