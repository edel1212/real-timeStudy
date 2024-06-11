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

    @Value("${redis.ssePrefix}")
    private String channelPrefix;

    // DB 대신 사용 중
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap<>();
    private final RedisMessageListenerContainer container;
    private final RedisSubscriber subscriber;


    public List<ChatRoom> findAllRoom() {
        List chatRooms = new ArrayList(chatRoomMap.values());
        // 최신 순으로 정렬
        Collections.reverse(chatRooms);
        return chatRooms;
    }

    /**
     * 채팅방 생성
     *  - redis에 구독
     */
    public ChatRoom createChatRoom(String roomName) {
        // 1 . UUID 생성 
        String roomId = UUID.randomUUID().toString();
        // 2 . 메세지 생성
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .name(roomName)
                .build();
        // 3 . TODO DB 대신 목록 생성을 위한 Map임 ::: 실제 로직 경우 dummy.save(~); 구현
        chatRoomMap.put(roomId, chatRoom);
        // 4 . Redis에 저장할 Topic명(식별키) 생성
        ChannelTopic channelTopic = ChannelTopic.of( channelPrefix + roomId);
        // 5 . 구독
        container.addMessageListener(subscriber, channelTopic);

        return chatRoom;
    }
}
