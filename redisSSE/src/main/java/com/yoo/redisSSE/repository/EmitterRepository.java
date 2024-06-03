package com.yoo.redisSSE.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class EmitterRepository {
    /**
     * 모든 Emitters를 저장하는 ConcurrentHashMap
     * - 추후 확정성을 위해 Reids로 변경하자
     * -  thread-safe를 위해 HashMap이 아닌 ConcurrentHashMap 사용
     * **/
    private final Map<String, SseEmitter> sseEmitter = new ConcurrentHashMap<>();

    /**
     * 주어진 아이디와 이미터를 저장
     *
     * @param accountId    - 사용자 아이디.
     * @param sseEmitter       - 이벤트 sseEmitter.
     */
    public SseEmitter save(String accountId, SseEmitter sseEmitter) {
        this.sseEmitter.put(accountId, sseEmitter);
        return sseEmitter;
    }

    public Optional<SseEmitter> findById(String memberId) {
        return Optional.ofNullable(this.sseEmitter.get(memberId));
    }

    /**
     * 주어진 아이디의 Emitter를 제거
     *
     * @param accountId - 사용자 아이디.
     */
    public void deleteById(String accountId) {
        this.sseEmitter.remove(accountId);
    }

    /**
     * 주어진 아이디의 Emitter를 가져옴.
     *
     * @param accountId     - 사용자 아이디.
     * @return SseEmitter   - 이벤트 Emitter.
     */
    public SseEmitter get(String accountId) {
        return this.sseEmitter.get(accountId);
    }
}
