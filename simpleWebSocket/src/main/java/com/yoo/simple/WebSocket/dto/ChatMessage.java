package com.yoo.simple.WebSocket.dto;

import com.yoo.simple.WebSocket.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ChatMessage {
    private ChatMessageType type;
    private String roomId;
    private String sender;
    private String message;
}
