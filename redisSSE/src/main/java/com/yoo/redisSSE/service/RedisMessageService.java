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
    // Redis에 구독을 하기위한 컨테이너 주입
    private final RedisMessageListenerContainer container;
    //
    private final RedisSubscriber subscriber;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 채널 구독
     * -
     * */
    public void subscribe(String channel) {
        container.addMessageListener(subscriber, ChannelTopic.of(getChannelName(channel)));
    }

    // 이벤트 발행
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
