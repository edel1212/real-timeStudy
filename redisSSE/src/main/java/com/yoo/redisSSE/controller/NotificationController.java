package com.yoo.redisSSE.controller;

import com.yoo.redisSSE.service.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationServiceImpl notificationService;

    @GetMapping(value = "/sub", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(String accountId){
        return notificationService.subscribe(accountId);
    }

    @PostMapping(value = "/send-data", produces = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void sendData(String accountId, String message) {
        notificationService.sendNotification(accountId, message);
    }
}