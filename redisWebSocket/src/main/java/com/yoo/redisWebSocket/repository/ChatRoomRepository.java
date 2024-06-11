package com.yoo.redisWebSocket.repository;

import com.yoo.redisWebSocket.dto.ChatRoom;
import com.yoo.redisWebSocket.service.RedisSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Repository
@RequiredArgsConstructor
public class ChatRoomRepository {

    // DB 대신 사용 중
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
    private final RedisMessageListenerContainer container;
    private final RedisSubscriber subscriber;

    @Value("${redis.ssePrefix}")
    private String channelPrefix;


    public List<ChatRoom> findAllRoom() {
        List chatRooms = new ArrayList(chatRoomMap.values());
        // 최신 순으로 정렬
        Collections.reverse(chatRooms);
        return chatRooms;
    }

    public ChatRoom createChatRoom(String roomName) {
        String roomId = UUID.randomUUID().toString();
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .name(roomName)
                .build();
        // DB 대신 목록 생성을 위한 Map임
        chatRoomMap.put(roomId, chatRoom);

        // 구독
        container.addMessageListener(subscriber, ChannelTopic.of(getChannelName(roomId)));

        return chatRoom;
    }

    private String getChannelName(String id) {
        return channelPrefix + id;
    }
}
