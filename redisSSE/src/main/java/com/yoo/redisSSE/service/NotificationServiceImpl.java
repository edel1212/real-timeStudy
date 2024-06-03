package com.yoo.redisSSE.service;

import com.yoo.redisSSE.repository.EmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Service
@Log4j2
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
    private final EmitterRepository emitterRepository;

    @Override
    public SseEmitter subscribe(String accountId) {
        SseEmitter emitter = createEmitter(accountId);
        log.info("--------------------------");
        log.info("EventStream Created. [userId=" + accountId + "]");
        log.info("--------------------------");
        sendToClient(accountId, "EventStream Created");
        return emitter;
    }

    @Override
    public void notify(String accountId, Object event) {
        this.sendToClient(accountId, event);
    }


    /**
     * 클라이언트에게 데이터를 전송
     *
     * @param accountId   - 데이터를 받을 사용자의 아이디.
     * @param data - 전송할 데이터.
     */
    private void sendToClient(String accountId, Object data) {
        SseEmitter emitter = emitterRepository.get(accountId);
        if (emitter == null) return;
        try {
            SseEmitter.SseEventBuilder  messageEvent
                    = SseEmitter.event()
                    .id(accountId)              // 메세지를 찾을 key
                    .name("sse")     // 이벤트명  : eventSource.addEventListener("sse",()=>{})
                    .data(data);                // 전달 데이터
            emitter.send(messageEvent);
        } catch (IOException exception) {
            emitterRepository.deleteById(accountId);
            emitter.completeWithError(exception);
        } // try - catch
    }

    /**
     * 사용자 아이디를 기반으로 이벤트 Emitter를 생성
     *
     * @param accountId - 사용자 아이디.
     * @return SseEmitter - 생성된 이벤트 Emitter.
     */
    private SseEmitter createEmitter(String accountId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(accountId, emitter);
        /**
         * ℹ️ 클라이언트가 SSE (Server-Sent Events) 스트림을 정상적으로 종료했을 때 발생합니다.
         *    이는 모든 데이터가 성공적으로 전송되고 더 이상 전송할 데이터가 없을 때 발생합니다.
         * */
        emitter.onCompletion(() -> emitterRepository.deleteById(accountId));
        /**
         * ℹ️ 설정된 시간 동안 이벤트를 전송하지 않았을 때 발생합니다.
         *     DEFAULT_TIMEOUT으로 설정된 시간이 지나면 타임아웃이 발생합니다.
         * */
        emitter.onTimeout(() -> emitterRepository.deleteById(accountId));
        return emitter;
    }

}