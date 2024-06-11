package com.yoo.redisWebSocket.controller;

import com.yoo.redisWebSocket.dto.ChatRoom;
import com.yoo.redisWebSocket.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ✅ 채팅 방을 관리하는 Controller 이다
 * */
@Log4j2
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> room() {
        List<ChatRoom> list = chatRoomRepository.findAllRoom();
        return ResponseEntity.ok(list);
    }

    // 채팅방 생성
    @PostMapping(value = "/room/{roomName}")
    public ChatRoom createRoom(@PathVariable String roomName) {
        return chatRoomRepository.createChatRoom(roomName);
    }

    // 특정 채팅방 조회
    @GetMapping("/room/{roomId}")
    public ChatRoom roomInfo(@PathVariable String roomId) {
        return chatRoomRepository.findRoomById(roomId);
    }

}
