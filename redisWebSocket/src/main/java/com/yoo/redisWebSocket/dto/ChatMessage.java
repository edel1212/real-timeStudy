package com.yoo.redisWebSocket.dto;

import com.yoo.redisWebSocket.enums.ChatMessageType;
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
