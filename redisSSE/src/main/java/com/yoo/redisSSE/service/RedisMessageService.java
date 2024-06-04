package com.yoo.redisSSE.service;

import com.yoo.redisSSE.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RedisMessageService {
    private final String CHANNEL_PREFIX = "emmit_";

    private final RedisMessageListenerContainer container;
    private final RedisSubscriber subscriber; // 따로 구현한 Subscriber
    private final RedisTemplate<String, Object> redisTemplate;

    // 채널 구독
    // 채널 구독
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
        return CHANNEL_PREFIX + id;
    }
}
