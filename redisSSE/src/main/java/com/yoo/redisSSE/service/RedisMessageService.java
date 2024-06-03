package com.yoo.redisSSE.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RedisMessageService {
    private final RedisMessageListenerContainer container;
    //private final RedisSubscriber subscriber; // 따로 구현한 Subscriber
    private final RedisTemplate<String, Object> redisTemplate;

//    public void subscribe(String channel) {
//        container.addMessageListener(subscriber, ChannelTopic.of(getChannelName(channel)));
//    }

    private String getChannelName(String id) {
        return "CHANNEL_PREFIX" + id;
    }

}
