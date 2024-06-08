package com.yoo.simple.WebSocket.controller;

import com.yoo.simple.WebSocket.dto.ChatRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ✅ 채팅 방을 관리하는 Controller 이다
 * */
@RequiredArgsConstructor
@RestController
@RequestMapping("/chat")
public class ChatRoomController {

    // 모든 채팅방 목록 반환
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> room() {
        //List<ChatRoom> list = chatRoomRepository.findAllRoom();
        return ResponseEntity.ok(null);
    }

    // 채팅방 생성
    @PostMapping("/room")
    public ChatRoom createRoom( String user1, String user2) {
        //return chatRoomRepository.createChatRoom(user1, user2);
        return  null;
    }


    // 특정 채팅방 조회
    @GetMapping("/room/{roomId}")
    public ChatRoom roomInfo(@PathVariable String roomId) {
        //return chatRoomRepository.findRoomById(roomId);
        return  null;
    }

}
