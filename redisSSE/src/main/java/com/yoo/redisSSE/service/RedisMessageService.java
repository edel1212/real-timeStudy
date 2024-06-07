package com.yoo.redisSSE.service;

import com.yoo.redisSSE.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RedisMessageService {
    // 채널에 사용할 Prefix - 강제는 아니다 SSE 이외 다양하게 RedisMessageListenerContainer를 사용하기 위함
    @Value("${redis.ssePrefix}")
    private String channelPrefix;
    /**
     * ℹ️ 메시지 리스너를 관리하고 조정하여, Redis 채널이나 패턴에 게시된 메시지에 반응할 수 있도록 함
     * - MessageListener 구현체를 등록하여, 들어오는 메시지를 애플리케이션이 어떻게 처리할지 정의합니다.
     * - 컨테이너는 리스너를 관리하고, 메시지가 도착하면 이를 적절한 리스너에게 전달합니다.
     * */
    private final RedisMessageListenerContainer container;
    /**
     * ℹ️ 메세지를 수신 및 처리를 담당
     * - RedisMessageListenerContainer에 주입될 구현체이다.
     *    - 해당 Class에 정의된 내용대로 메세지를 처리한다.
     * */
    private final RedisSubscriber subscriber;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 채널 구독
     * */
    public void subscribe(String channel) {
        //  Redis의 지정 채널에 메시지가 게시될 때마다 MessageListener를 구현한 Class가 해당 메시지를 처리함
        container.addMessageListener(subscriber, ChannelTopic.of(getChannelName(channel)));
    }

    /**
     * Reids에 저장된 채널에 이벤트 발행
     * */
    public void publish(String channel, NotificationDto notificationDto) {
        redisTemplate.convertAndSend(getChannelName(channel), notificationDto);
    }

    // 구독 삭제
    public void removeSubscribe(String channel) {
        container.removeMessageListener(subscriber, ChannelTopic.of(getChannelName(channel)));
    }

    private String getChannelName(String id) {
        return channelPrefix + id;
    }
}
