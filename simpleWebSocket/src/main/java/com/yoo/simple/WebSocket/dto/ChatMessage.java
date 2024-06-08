package com.yoo.simple.WebSocket.dto;

import com.yoo.simple.WebSocket.enums.ChatMessageType;

public class ChatMessage {
    private ChatMessageType type;
    private String roomId;
    private String sender;
    private String message;
}
