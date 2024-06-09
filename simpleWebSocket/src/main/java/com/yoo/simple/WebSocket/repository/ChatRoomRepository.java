package com.yoo.simple.WebSocket.repository;

import com.yoo.simple.WebSocket.dto.ChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Repository
@RequiredArgsConstructor
public class ChatRoomRepository {
    private final Map<String, ChatRoom> chatRoomMap = new ConcurrentHashMap();

    public List<ChatRoom> findAllRoom() {
        List chatRooms = new ArrayList(chatRoomMap.values());
        // 최신 순으로 정렬
        Collections.reverse(chatRooms);
        return chatRooms;
    }

    public ChatRoom findRoomById(String id) {
        return chatRoomMap.get(id);
    }

    //
    public ChatRoom createChatRoom(String roomName) {
        String roomId = UUID.randomUUID().toString();
        log.info("-----------");
        log.info("roomId ::: {}",roomId);
        log.info("-----------");
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .name(roomName)
                .build();
        chatRoomMap.put(roomId, chatRoom);
        return chatRoom;
    }


}
